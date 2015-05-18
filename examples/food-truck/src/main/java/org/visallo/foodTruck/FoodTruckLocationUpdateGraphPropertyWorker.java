package org.visallo.foodTruck;

import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.twitter.TwitterOntology;
import org.vertexium.*;
import org.vertexium.type.GeoPoint;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

import static org.vertexium.util.IterableUtils.*;

public class FoodTruckLocationUpdateGraphPropertyWorker extends GraphPropertyWorker {
    private static final String MULTI_VALUE_KEY = FoodTruckLocationUpdateGraphPropertyWorker.class.getName();

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        Edge hasKeywordEdge = (Edge) data.getElement();

        Vertex tweetVertex = hasKeywordEdge.getVertex(Direction.OUT, getAuthorizations());
        if (isRetweet(tweetVertex)) {
            return;
        }

        Vertex keywordVertex = hasKeywordEdge.getVertex(Direction.IN, getAuthorizations());
        Vertex tweeter = single(tweetVertex.getVertices(Direction.BOTH, TwitterOntology.EDGE_LABEL_TWEETED, getAuthorizations()));
        Vertex foodTruck = singleOrDefault(tweeter.getVertices(Direction.BOTH, FoodTruckOntology.EDGE_LABEL_HAS_TWITTER_USER, getAuthorizations()), null);
        if (foodTruck == null) {
            return;
        }

        String keywordTitle = VisalloProperties.TITLE.getOnlyPropertyValue(keywordVertex);
        GeoPoint geoLocation = FoodTruckOntology.GEO_LOCATION.getOnlyPropertyValue(keywordVertex);
        if (geoLocation != null) {
            Date geoLocationDate = VisalloProperties.PUBLISHED_DATE.getOnlyPropertyValue(tweetVertex);
            Date currentGetLocationDate = FoodTruckOntology.GEO_LOCATION_DATE.getOnlyPropertyValue(foodTruck);
            if (currentGetLocationDate == null || geoLocationDate.compareTo(currentGetLocationDate) > 0) {
                Calendar geoLocationCalendar = Calendar.getInstance();
                geoLocationCalendar.setTime(geoLocationDate);

                Calendar nowCalendar = Calendar.getInstance();
                nowCalendar.setTime(new Date());

                if (geoLocationCalendar.get(Calendar.DAY_OF_YEAR) != nowCalendar.get(Calendar.DAY_OF_YEAR)) {
                    return;
                }

                geoLocation = new GeoPoint(geoLocation.getLatitude(), geoLocation.getLongitude(), geoLocation.getAltitude(), keywordTitle);
                FoodTruckOntology.GEO_LOCATION.addPropertyValue(foodTruck, MULTI_VALUE_KEY, geoLocation, data.getVisibility(), getAuthorizations());
                FoodTruckOntology.GEO_LOCATION_DATE.addPropertyValue(foodTruck, MULTI_VALUE_KEY, geoLocationDate, data.getVisibility(), getAuthorizations());
                getGraph().flush();
                getWorkQueueRepository().pushGraphPropertyQueue(foodTruck, FoodTruckOntology.GEO_LOCATION.getFirstProperty(foodTruck), data.getPriority());
            }
        }
    }

    private boolean isRetweet(Vertex tweetVertex) {
        return count(tweetVertex.getEdges(Direction.IN, TwitterOntology.EDGE_LABEL_RETWEET, getAuthorizations())) > 0;
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (!(element instanceof Edge)) {
            return false;
        }

        Edge edge = (Edge) element;
        if (!edge.getLabel().equals(FoodTruckOntology.EDGE_LABEL_HAS_KEYWORD)) {
            return false;
        }

        return true;
    }
}
