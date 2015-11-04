package org.visallo.geocoder.bing;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.vertexium.*;
import org.vertexium.type.GeoPoint;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.externalResource.QueueExternalResourceWorker;
import org.visallo.core.geocoding.GeocodeResult;
import org.visallo.core.geocoding.GeocoderRepository;
import org.visallo.core.model.Description;
import org.visallo.core.model.FlushFlag;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.types.GeoPointVisalloProperty;
import org.visallo.core.model.workQueue.Priority;

import java.util.List;

@Name("Bing Geo-Locator")
@Description("Uses Bing to geo-locate a string")
public class BingExternalResourceWorker extends QueueExternalResourceWorker {
    public static final String QUEUE_NAME = QUEUE_NAME_PREFIX + "Bing";
    public static final int DEFAULT_MULTIPLE_MATCH_FUZZINESS_KM = 20;
    private final Graph graph;
    private double multipleMatchFuzzinessKm;

    @Inject
    public BingExternalResourceWorker(
            Graph graph,
            Configuration configuration
    ) {
        this.graph = graph;
        this.multipleMatchFuzzinessKm = configuration.getInt(BingExternalResourceWorker.class.getName() + ".multipleMatchFuzzinessKm", DEFAULT_MULTIPLE_MATCH_FUZZINESS_KM);
    }

    public void queuePropertySet(
            String locationString,
            ElementType elementType,
            String elementId,
            String propertyKey,
            String propertyName,
            Visibility visibility,
            Priority priority
    ) {
        JSONObject json = new JSONObject();
        json.put("type", WorkType.PROPERTY_SET.name());
        json.put("locationString", locationString);
        json.put("elementType", elementType.name());
        json.put("elementId", elementId);
        json.put("propertyKey", propertyKey);
        json.put("propertyName", propertyName);
        json.put("visibility", visibility.getVisibilityString());
        json.put("priority", priority.name());
        getWorkQueueRepository().pushOnQueue(getQueueName(), FlushFlag.DEFAULT, json, priority);
    }

    @Override
    protected void process(Object messageId, JSONObject json, Authorizations authorizations) throws Exception {
        WorkType type = WorkType.valueOf(json.getString("type"));
        switch (type) {
            case PROPERTY_SET:
                processPropertySet(json, authorizations);
                break;
            default:
                throw new VisalloException("Unhandled type: " + type);
        }
    }

    private void processPropertySet(JSONObject json, Authorizations authorizations) {
        String locationString = json.getString("locationString");
        ElementType elementType = ElementType.valueOf(json.getString("elementType"));
        String elementId = json.getString("elementId");
        String propertyKey = json.getString("propertyKey");
        String propertyName = json.getString("propertyName");
        Visibility visibility = new Visibility(json.getString("visibility"));
        Priority priority = Priority.safeParse(json.optString("priority"));
        GeoPointVisalloProperty property = new GeoPointVisalloProperty(propertyName);
        processPropertySet(locationString, elementType, elementId, propertyKey, property, visibility, priority, authorizations);
    }

    private void processPropertySet(
            String locationString,
            ElementType elementType,
            String elementId,
            String propertyKey,
            GeoPointVisalloProperty property,
            Visibility visibility,
            Priority priority,
            Authorizations authorizations
    ) {
        List<GeocodeResult> results = InjectHelper.getInstance(GeocoderRepository.class).find(locationString);
        if (results.size() == 0) {
            return;
        }
        GeocodeResult result;
        if (results.size() > 1) {
            result = tryAverageResults(results);
            if (result == null) {
                return;
            }
        } else {
            result = results.get(0);
        }

        Element element;
        if (elementType == ElementType.VERTEX) {
            element = graph.getVertex(elementId, authorizations);
        } else if (elementType == ElementType.EDGE) {
            element = graph.getEdge(elementId, authorizations);
        } else {
            throw new VisalloException("Unhandled element type: " + elementType);
        }

        double latitude = result.getLatitude();
        double longitude = result.getLongitude();
        String description = result.getName();
        GeoPoint value = new GeoPoint(latitude, longitude, description);
        property.addPropertyValue(element, propertyKey, value, visibility, authorizations);
        graph.flush();
        getWorkQueueRepository().pushGraphPropertyQueue(element, property.getProperty(element, propertyKey), priority);
    }

    private GeocodeResult tryAverageResults(List<GeocodeResult> results) {
        GeocodeResult firstResult = results.get(0);
        for (GeocodeResult result : results) {
            double distanceKm = GeoPoint.distanceBetween(firstResult.getLatitude(), firstResult.getLongitude(), result.getLatitude(), result.getLongitude());
            if (distanceKm > multipleMatchFuzzinessKm) {
                return null;
            }
        }
        return firstResult;
    }

    @Override
    public String getQueueName() {
        return QUEUE_NAME;
    }

    private enum WorkType {
        PROPERTY_SET
    }
}
