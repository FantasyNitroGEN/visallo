package org.visallo.flightTrack;

import org.visallo.core.model.properties.types.DoubleVisalloProperty;
import org.visallo.core.model.properties.types.GeoPointVisalloProperty;
import org.visallo.core.model.properties.types.StringVisalloProperty;

public class FlightTrackOntology {
    public static final String EDGE_LABEL_HAS_ORIGIN = "http://visallo.org/flightTrack#airplaneOrigin";
    public static final String EDGE_LABEL_HAS_AIRPLANE = "http://visallo.org/flightTrack#airlineHasAirplane";
    public static final String EDGE_LABEL_HAS_DESTINATION = "http://visallo.org/flightTrack#airplaneDestination";

    public static final String CONCEPT_TYPE_AIRPORT = "http://visallo.org/flightTrack#airport";
    public static final String CONCEPT_TYPE_AIRPLANE = "http://visallo.org/flightTrack#airplane";
    public static final String CONCEPT_TYPE_FLIGHT_TRACK = "http://visallo.org/flightTrack#flightTrack";
    public static final String CONCEPT_TYPE_AIRLINE = "http://visallo.org/flightTrack#airline";

    public static final StringVisalloProperty IDENT = new StringVisalloProperty("http://visallo.org/flightTrack#ident");
    public static final StringVisalloProperty AIRPORT_CODE = new StringVisalloProperty("http://visallo.org/flightTrack#airportCode");
    public static final DoubleVisalloProperty HEADING = new DoubleVisalloProperty("http://visallo.org/flightTrack#heading");
    public static final StringVisalloProperty AIRLINE_PREFIX = new StringVisalloProperty("http://visallo.org/flightTrack#airlinePrefix");
    public static final DoubleVisalloProperty ALTITUDE = new DoubleVisalloProperty("http://visallo.org/flightTrack#altitude");
    public static final GeoPointVisalloProperty LOCATION = new GeoPointVisalloProperty("http://visallo.org/flightTrack#geoLocation");
}
