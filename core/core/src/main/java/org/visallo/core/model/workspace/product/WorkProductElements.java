package org.visallo.core.model.workspace.product;

import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.*;
import org.visallo.core.model.graph.ElementUpdateContext;
import org.visallo.core.user.User;
import org.visallo.core.util.JSONUtil;

import java.util.List;

public abstract class WorkProductElements implements WorkProduct, WorkProductHasElements {

    public static final String WORKSPACE_PRODUCT_TO_ENTITY_RELATIONSHIP_IRI = "http://visallo.org/workspace/product#toEntity";


        @Override
        public void update(JSONObject params, Graph graph, Vertex workspaceVertex, ElementUpdateContext<Vertex> vertexBuilder, User user, Visibility visibility, Authorizations authorizations) {
            JSONObject updateVertices = params.optJSONObject("updateVertices");
            if (updateVertices != null) {
                Vertex productVertex = graph.getVertex(vertexBuilder.getElement().getId(), authorizations);
                List<String> vertexIds = Lists.newArrayList(updateVertices.keys());
                for (String id : vertexIds) {
                    String edgeId = getEdgeId(productVertex.getId(), id);
                    EdgeBuilderByVertexId edgeBuilder = graph.prepareEdge(edgeId, productVertex.getId(), id, WORKSPACE_PRODUCT_TO_ENTITY_RELATIONSHIP_IRI, visibility);
                    updateProductEdge(updateVertices.getJSONObject(id), edgeBuilder, visibility);
                    edgeBuilder.save(authorizations);
                }
            }
            JSONArray removeVertices = params.optJSONArray("removeVertices");
            if (removeVertices != null) {
                JSONUtil.toList(removeVertices).stream().forEach(id -> {
                    graph.softDeleteEdge(getEdgeId(vertexBuilder.getElement().getId(), (String) id), authorizations);
                });
            }
        }

        @Override
        public JSONObject get(JSONObject params, Graph graph, Vertex workspaceVertex, Vertex productVertex, User user, Authorizations authorizations) {
            JSONObject extendedData = new JSONObject();

            boolean includeVertices = params.optBoolean("includeVertices");
            boolean includeEdges = params.optBoolean("includeEdges");
            String id = productVertex.getId();

            if (includeVertices || includeEdges) {
                List<String> vertexIds = Lists.newArrayList(productVertex.getVertexIds(Direction.OUT, WORKSPACE_PRODUCT_TO_ENTITY_RELATIONSHIP_IRI, authorizations));

                JSONArray edges = new JSONArray();
                JSONArray vertices = new JSONArray();

                if (includeVertices) {
                    Iterable<Edge> productVertexEdges = productVertex.getEdges(Direction.OUT, WORKSPACE_PRODUCT_TO_ENTITY_RELATIONSHIP_IRI, authorizations);
                    for (Edge propertyVertexEdge : productVertexEdges) {
                        String other = propertyVertexEdge.getOtherVertexId(id);
                        if (includeVertices) {
                            JSONObject vertex = new JSONObject();
                            vertex.put("id", other);
                            setEdgeJson(propertyVertexEdge, vertex);
                            vertices.put(vertex);
                        }
                    }
                    extendedData.put("vertices", vertices);
                }

                if (includeEdges) {
                    Iterable<RelatedEdge> relatedEdges = graph.findRelatedEdgeSummary(vertexIds, authorizations);
                    for (RelatedEdge relatedEdge : relatedEdges) {
                        if (includeEdges) {
                            JSONObject edge = new JSONObject();
                            edge.put("edgeId", relatedEdge.getEdgeId());
                            edge.put("label", relatedEdge.getLabel());
                            edge.put("outVertexId", relatedEdge.getOutVertexId());
                            edge.put("inVertexId", relatedEdge.getInVertexId());
                            edges.put(edge);
                        }
                    }
                    extendedData.put("edges", edges);
                }
            }

            return extendedData;
        }

        protected abstract void setEdgeJson(Edge propertyVertexEdge, JSONObject vertex);
        protected abstract void updateProductEdge(JSONObject update, ElementBuilder edgeBuilder, Visibility visibility);


        private String getEdgeId(String productId, String vertexId) {
            return productId + "_hasVertex_" + vertexId;
        }

}
