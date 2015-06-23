package org.visallo.flightTrack;

import com.google.inject.Inject;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;
import org.vertexium.*;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.type.GeoPoint;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.vertexium.util.IterableUtils.toList;

public class FlightRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(FlightRepository.class);
    public static final SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");
    private static final String MULTI_VALUE_PROPERTY_KEY = FlightRepository.class.getName();
    private static final String SOURCE_NAME = "FlightAware.com";
    private Graph graph;
    private WorkQueueRepository workQueueRepository;
    private Map<String, Airport> airportCodeMap = new HashMap<>();
    private Map<String, Airline> identPrefixMap = new HashMap<>();
    private Map<String, Vertex> identToVertex = new HashMap<>();
    private Map<String, Vertex> airportCodeToVertex = new HashMap<>();
    private Map<String, Vertex> airlinePrefixToVertex = new HashMap<>();

    public FlightRepository() {
        try {
            loadAirlines();
            loadAirports();
        } catch (IOException ex) {
            throw new VisalloException("Could not read dat files", ex);
        }
    }

    private void loadAirports() throws IOException {
        InputStreamReader reader = new InputStreamReader(FlightRepository.class.getResourceAsStream("airports.dat"));
        CsvListReader listReader = new CsvListReader(reader, CsvPreference.STANDARD_PREFERENCE);
        List<String> line;
        while ((line = listReader.read()) != null) {
            String title = line.get(1);
            String airportCode = line.get(5);
            double lat = Double.parseDouble(line.get(6));
            double lon = Double.parseDouble(line.get(7));
            if (airportCode == null) {
                continue;
            }
            airportCodeMap.put(airportCode.toLowerCase(), new Airport(airportCode, title, new GeoPoint(lat, lon)));
        }
    }

    private void loadAirlines() throws IOException {
        InputStreamReader reader = new InputStreamReader(FlightRepository.class.getResourceAsStream("airlines.dat"));
        CsvListReader listReader = new CsvListReader(reader, CsvPreference.STANDARD_PREFERENCE);
        List<String> line;
        while ((line = listReader.read()) != null) {
            String title = line.get(1);
            String identPrefix = line.get(4);
            if (identPrefix == null) {
                continue;
            }
            identPrefixMap.put(identPrefix.toLowerCase(), new Airline(identPrefix, title));
        }
    }

    public void save(JSONObject json, Visibility visibility, Priority priority, Authorizations authorizations) {
        JSONArray aircraft = json
                .getJSONObject("SearchResult")
                .getJSONArray("aircraft");
        for (int i = 0; i < aircraft.length(); i++) {
            JSONObject flight = aircraft.getJSONObject(i);
            saveFlightData(flight, visibility, priority, authorizations);
        }
    }

    public void saveFlightData(JSONObject flightJson, Visibility visibility, Priority priority, Authorizations authorizations) {
        double latitude = flightJson.getDouble("latitude");
        double longitude = flightJson.getDouble("longitude");
        double altitude = flightJson.getDouble("altitude");
        double heading = flightJson.getDouble("heading");
        String ident = flightJson.getString("ident");
        String origin = flightJson.getString("origin");
        String destination = flightJson.getString("destination");

        Vertex airplaneVertex = saveAirplane(ident, visibility, priority, authorizations);
        Vertex originVertex = saveAirport(origin, visibility, priority, authorizations);
        Vertex destinationVertex = saveAirport(destination, visibility, priority, authorizations);

        if (updateOrigin(airplaneVertex, originVertex, visibility, authorizations)
                || updateDestination(airplaneVertex, destinationVertex, visibility, authorizations)) {
            identToVertex.remove(ident); // edges are now invalid in the cache and we need to refresh
        }

        updateLocation(airplaneVertex, latitude, longitude, altitude, heading, visibility, priority, authorizations);
    }

    public void updateLocation(Vertex airplaneVertex, double latitude, double longitude, double altitude, double heading, Visibility visibility, Priority priority, Authorizations authorizations) {
        LOGGER.debug("updating location of airplane %s (lat: %f, lon: %f, alt: %f, head: %f)", airplaneVertex.getId(), latitude, longitude, altitude, heading);

        ExistingElementMutation<Vertex> m = airplaneVertex.prepareMutation();
        FlightTrackOntology.LOCATION.addPropertyValue(m, MULTI_VALUE_PROPERTY_KEY, new GeoPoint(latitude, longitude, altitude), visibility);
        FlightTrackOntology.ALTITUDE.addPropertyValue(m, MULTI_VALUE_PROPERTY_KEY, altitude, visibility);
        FlightTrackOntology.HEADING.addPropertyValue(m, MULTI_VALUE_PROPERTY_KEY, heading, visibility);
        m.save(authorizations);

        graph.flush();

        workQueueRepository.pushGraphPropertyQueue(airplaneVertex, MULTI_VALUE_PROPERTY_KEY, FlightTrackOntology.LOCATION.getPropertyName(), priority);
        workQueueRepository.pushGraphPropertyQueue(airplaneVertex, MULTI_VALUE_PROPERTY_KEY, FlightTrackOntology.ALTITUDE.getPropertyName(), priority);
        workQueueRepository.pushGraphPropertyQueue(airplaneVertex, MULTI_VALUE_PROPERTY_KEY, FlightTrackOntology.HEADING.getPropertyName(), priority);
    }

    public boolean updateDestination(Vertex airplaneVertex, Vertex destinationVertex, Visibility visibility, Authorizations authorizations) {
        List<String> currentDestinations = toList(airplaneVertex.getVertexIds(Direction.BOTH, FlightTrackOntology.EDGE_LABEL_HAS_DESTINATION, authorizations));
        if (currentDestinations.size() == 0 || !currentDestinations.get(0).equals(destinationVertex.getId())) {
            LOGGER.debug("airplane %s changed destinations to %s", airplaneVertex.getId(), destinationVertex.getId());
            for (Object currentDestinationEdgeId : airplaneVertex.getEdgeIds(Direction.BOTH, FlightTrackOntology.EDGE_LABEL_HAS_DESTINATION, authorizations)) {
                graph.softDeleteEdge((String) currentDestinationEdgeId, authorizations);
            }
            Edge e = graph.addEdge(toDestinationEdgeId(airplaneVertex, destinationVertex), airplaneVertex, destinationVertex, FlightTrackOntology.EDGE_LABEL_HAS_DESTINATION, visibility, authorizations);
            graph.flush();
            workQueueRepository.pushElement(e);
            return true;
        }
        return false;
    }

    public boolean updateOrigin(Vertex airplaneVertex, Vertex originVertex, Visibility visibility, Authorizations authorizations) {
        List<String> currentOrigins = toList(airplaneVertex.getVertexIds(Direction.BOTH, FlightTrackOntology.EDGE_LABEL_HAS_ORIGIN, authorizations));
        if (currentOrigins.size() == 0 || !currentOrigins.get(0).equals(originVertex.getId())) {
            LOGGER.debug("airplane %s changed origin to %s", airplaneVertex.getId(), originVertex.getId());
            for (Object currentOriginEdgeId : airplaneVertex.getEdgeIds(Direction.BOTH, FlightTrackOntology.EDGE_LABEL_HAS_ORIGIN, authorizations)) {
                graph.softDeleteEdge((String) currentOriginEdgeId, authorizations);
            }
            Edge e = graph.addEdge(toOriginEdgeId(airplaneVertex, originVertex), airplaneVertex, originVertex, FlightTrackOntology.EDGE_LABEL_HAS_ORIGIN, visibility, authorizations);
            graph.flush();
            workQueueRepository.pushElement(e);
            return true;
        }
        return false;
    }

    private Vertex saveAirport(String airportCode, Visibility visibility, Priority priority, Authorizations authorizations) {
        Vertex v = airportCodeToVertex.get(airportCode);
        if (v != null) {
            return v;
        }

        String airportId = toAirportId(airportCode);
        v = graph.getVertex(airportId, authorizations);
        if (v != null) {
            airportCodeToVertex.put(airportCode, v);
            return v;
        }

        LOGGER.info("new airport %s", airportCode);

        Airport airport = airportCodeMap.get(airportCode.toLowerCase());

        VertexBuilder vb = graph.prepareVertex(airportId, visibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(vb, FlightTrackOntology.CONCEPT_TYPE_AIRPORT, visibility);
        FlightTrackOntology.AIRPORT_CODE.addPropertyValue(vb, MULTI_VALUE_PROPERTY_KEY, airportCode, visibility);
        VisalloProperties.SOURCE.addPropertyValue(vb, MULTI_VALUE_PROPERTY_KEY, SOURCE_NAME, visibility);
        if (airport != null) {
            FlightTrackOntology.LOCATION.addPropertyValue(vb, MULTI_VALUE_PROPERTY_KEY, airport.getGeoPoint(), visibility);
            FlightTrackOntology.AIRPORT_NAME.addPropertyValue(vb, MULTI_VALUE_PROPERTY_KEY, airport.getTitle(), visibility);
        }
        v = vb.save(authorizations);
        airportCodeToVertex.put(airportCode, v);

        graph.flush();

        workQueueRepository.pushElement(v);
        workQueueRepository.pushGraphPropertyQueue(v, MULTI_VALUE_PROPERTY_KEY, FlightTrackOntology.AIRPORT_CODE.getPropertyName(), priority);

        return v;
    }

    private Vertex saveAirplane(String ident, Visibility visibility, Priority priority, Authorizations authorizations) {
        Vertex airplaneVertex = identToVertex.get(ident);
        if (airplaneVertex != null) {
            return airplaneVertex;
        }

        String airplaneId = toAirplaneId(ident);
        airplaneVertex = graph.getVertex(airplaneId, authorizations);
        if (airplaneVertex != null) {
            identToVertex.put(ident, airplaneVertex);
            return airplaneVertex;
        }

        LOGGER.info("new airplane %s", ident);

        VertexBuilder vb = graph.prepareVertex(airplaneId, visibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(vb, FlightTrackOntology.CONCEPT_TYPE_AIRPLANE, visibility);
        FlightTrackOntology.IDENT.addPropertyValue(vb, MULTI_VALUE_PROPERTY_KEY, ident, visibility);
        VisalloProperties.SOURCE.addPropertyValue(vb, MULTI_VALUE_PROPERTY_KEY, SOURCE_NAME, visibility);
        airplaneVertex = vb.save(authorizations);
        identToVertex.put(ident, airplaneVertex);
        graph.flush();

        workQueueRepository.pushElement(airplaneVertex);
        workQueueRepository.pushGraphPropertyQueue(airplaneVertex, MULTI_VALUE_PROPERTY_KEY, FlightTrackOntology.IDENT.getPropertyName(), priority);

        Vertex airlineVertex = findAirlineVertexFromIdent(ident, visibility, priority, authorizations);
        graph.flush();

        if (airlineVertex != null) {
            String airlineHasAirplaneId = toAirlineHasAirplaneId(airlineVertex, airplaneVertex);

            if (graph.getEdge(airlineHasAirplaneId, authorizations) == null) {
                Edge e = graph.addEdge(airlineHasAirplaneId, airlineVertex, airplaneVertex, FlightTrackOntology.EDGE_LABEL_HAS_AIRPLANE, visibility, authorizations);
                graph.flush();
                workQueueRepository.pushElement(e);
            }
        }

        return airplaneVertex;
    }

    private Vertex findAirlineVertexFromIdent(String ident, Visibility visibility, Priority priority, Authorizations authorizations) {
        Airline airline = findAirlineFromIdent(ident);
        if (airline == null) {
            return null;
        }

        Vertex v = airlinePrefixToVertex.get(airline.getIdentPrefix());
        if (v != null) {
            return v;
        }

        String airlineId = toAirlineId(airline.getIdentPrefix());
        v = graph.getVertex(airlineId, authorizations);
        if (v != null) {
            airlinePrefixToVertex.put(airline.getIdentPrefix(), v);
            return v;
        }

        LOGGER.info("new airline %s", airline.getTitle());

        VertexBuilder vb = graph.prepareVertex(airlineId, visibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(vb, FlightTrackOntology.CONCEPT_TYPE_AIRLINE, visibility);
        FlightTrackOntology.AIRLINE_NAME.addPropertyValue(vb, MULTI_VALUE_PROPERTY_KEY, airline.getTitle(), visibility);
        FlightTrackOntology.AIRLINE_PREFIX.addPropertyValue(vb, MULTI_VALUE_PROPERTY_KEY, airline.getIdentPrefix(), visibility);
        VisalloProperties.SOURCE.addPropertyValue(vb, MULTI_VALUE_PROPERTY_KEY, SOURCE_NAME, visibility);
        v = vb.save(authorizations);
        airlinePrefixToVertex.put(airline.getIdentPrefix(), v);

        graph.flush();

        workQueueRepository.pushElement(v);
        workQueueRepository.pushGraphPropertyQueue(v, MULTI_VALUE_PROPERTY_KEY, FlightTrackOntology.AIRLINE_NAME.getPropertyName(), priority);
        workQueueRepository.pushGraphPropertyQueue(v, MULTI_VALUE_PROPERTY_KEY, FlightTrackOntology.AIRLINE_PREFIX.getPropertyName(), priority);

        return v;
    }

    private Airline findAirlineFromIdent(String ident) {
        ident = ident.toLowerCase();
        while (ident.length() > 1) {
            Airline airline = identPrefixMap.get(ident);
            if (airline != null) {
                return airline;
            }
            ident = ident.substring(0, ident.length() - 1);
        }
        return null;
    }

    private String toAirplaneId(String ident) {
        return "FlightTrack_Airplane_" + ident;
    }

    private String toAirportId(String airportCode) {
        return "FlightTrack_Airport_" + airportCode;
    }

    private String toAirlineId(String identPrefix) {
        return "FlightTrack_Airline_" + identPrefix;
    }

    private String toAirlineHasAirplaneId(Vertex airlineVertex, Vertex airplaneVertex) {
        return airlineVertex.getId() + "_has_" + airplaneVertex;
    }

    private String toOriginEdgeId(Vertex airplaneVertex, Vertex destinationVertex) {
        return airplaneVertex.getId() + "_has_org_" + destinationVertex.getId();
    }

    private String toDestinationEdgeId(Vertex airplaneVertex, Vertex destinationVertex) {
        return airplaneVertex.getId() + "_has_dest_" + destinationVertex.getId();
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Inject
    public void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    private static class Airport {
        private final String airportCode;
        private final String title;
        private final GeoPoint geoPoint;

        public Airport(String airportCode, String title, GeoPoint geoPoint) {
            this.airportCode = airportCode;
            this.title = title;
            this.geoPoint = geoPoint;
        }

        public String getAirportCode() {
            return airportCode;
        }

        public String getTitle() {
            return title;
        }

        public GeoPoint getGeoPoint() {
            return geoPoint;
        }
    }

    private static class Airline {
        private final String identPrefix;
        private final String title;

        public Airline(String identPrefix, String title) {
            this.identPrefix = identPrefix;
            this.title = title;
        }

        public String getIdentPrefix() {
            return identPrefix;
        }

        public String getTitle() {
            return title;
        }
    }
}
