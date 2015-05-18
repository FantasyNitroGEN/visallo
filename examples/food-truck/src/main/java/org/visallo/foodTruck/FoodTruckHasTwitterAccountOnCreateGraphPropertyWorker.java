package org.visallo.foodTruck;

import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.model.properties.VisalloProperties;
import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;

import java.io.InputStream;

public class FoodTruckHasTwitterAccountOnCreateGraphPropertyWorker extends GraphPropertyWorker {
    private static final String MULTI_VALUE_KEY = FoodTruckHasTwitterAccountOnCreateGraphPropertyWorker.class.getName();

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        Edge hasTwitterUserEdge = (Edge) data.getElement();

        Vertex foodTruckVertex = hasTwitterUserEdge.getVertex(Direction.OUT, getAuthorizations());
        Vertex twitterUserVertex = hasTwitterUserEdge.getVertex(Direction.IN, getAuthorizations());

        String imageVertexId = VisalloProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyValue(twitterUserVertex);
        if (imageVertexId != null && imageVertexId.length() > 0) {
            VisalloProperties.ENTITY_IMAGE_VERTEX_ID.setProperty(foodTruckVertex, imageVertexId, new Visibility(data.getVisibilitySource()), getAuthorizations());
            getGraph().flush();
            getWorkQueueRepository().pushGraphPropertyQueue(foodTruckVertex, ElementMutation.DEFAULT_KEY, VisalloProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName(), data.getPriority());
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (!(element instanceof Edge)) {
            return false;
        }

        Edge edge = (Edge) element;
        if (!edge.getLabel().equals(FoodTruckOntology.EDGE_LABEL_HAS_TWITTER_USER)) {
            return false;
        }

        return true;
    }
}
