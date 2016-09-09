package org.visallo.web.product.graph;

import org.json.JSONObject;
import org.vertexium.Edge;
import org.vertexium.ElementBuilder;
import org.vertexium.Visibility;
import org.visallo.core.model.workspace.product.WorkProductElements;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public class GraphWorkProduct extends WorkProductElements {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GraphWorkProduct.class);
    public static final String EDGE_POSITION = "http://visallo.org/workspace/product/graph#entityPosition";

    @Override
    protected void updateProductEdge(JSONObject update, ElementBuilder edgeBuilder, Visibility visibility) {
        JSONObject position = update;
        edgeBuilder.setProperty(EDGE_POSITION, position.toString(), visibility);
    }

    protected void setEdgeJson(Edge propertyVertexEdge, JSONObject vertex) {
        String positionStr = (String) propertyVertexEdge.getPropertyValue(EDGE_POSITION);
        JSONObject position;
        if (positionStr == null) {
            position = new JSONObject();
            position.put("x", 0);
            position.put("y", 0);
        } else {
            position = new JSONObject(positionStr);
        }
        vertex.put("pos", position);
    }

}
