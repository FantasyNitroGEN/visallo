package org.visallo.core.model.workspace.product;

import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.*;
import org.visallo.core.model.graph.ElementUpdateContext;
import org.visallo.core.model.graph.GraphUpdateContext;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.workspace.WorkspaceProperties;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.User;
import org.visallo.core.util.StreamUtil;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class WorkProductElements implements WorkProduct, WorkProductHasElements {
    private final OntologyRepository ontologyRepository;
    private final AuthorizationRepository authorizationRepository;

    protected WorkProductElements(
            OntologyRepository ontologyRepository,
            AuthorizationRepository authorizationRepository
    ) {
        this.ontologyRepository = ontologyRepository;
        this.authorizationRepository = authorizationRepository;
    }

    @Override
    public JSONObject getExtendedData(
            Graph graph,
            Vertex workspaceVertex,
            Vertex productVertex,
            JSONObject params,
            User user,
            Authorizations authorizations
    ) {
        JSONObject extendedData = new JSONObject();
        String id = productVertex.getId();

        if (params.optBoolean("includeVertices")) {
            JSONObject vertices = new JSONObject();
            List<Edge> productVertexEdges = Lists.newArrayList(productVertex.getEdges(
                    Direction.OUT,
                    WorkspaceProperties.PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                    authorizations
            ));

            List<String> ids = productVertexEdges.stream()
                    .map(edge -> edge.getOtherVertexId(id))
                    .collect(Collectors.toList());
            Map<String, Vertex> othersById = StreamUtil.stream(graph.getVertices(ids, FetchHint.NONE, authorizations))
                    .collect(Collectors.toMap(Vertex::getId, Function.identity()));

            for (Edge propertyVertexEdge : productVertexEdges) {
                String otherId = propertyVertexEdge.getOtherVertexId(id);
                JSONObject vertex = new JSONObject();
                vertex.put("id", otherId);
                if (!othersById.containsKey(otherId)) {
                    vertex.put("unauthorized", true);
                }
                setEdgeJson(propertyVertexEdge, vertex);
                vertices.put(otherId, vertex);
            }
            extendedData.put("vertices", vertices);
        }

        if (params.optBoolean("includeEdges")) {
            JSONObject edges = new JSONObject();
            Authorizations systemAuthorizations = authorizationRepository.getGraphAuthorizations(
                    user,
                    VisalloVisibility.SUPER_USER_VISIBILITY_STRING
            );
            Iterable<Vertex> productVertices = Lists.newArrayList(productVertex.getVertices(
                    Direction.OUT,
                    WorkspaceProperties.PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                    systemAuthorizations
            ));
            Iterable<RelatedEdge> productRelatedEdges = graph.findRelatedEdgeSummaryForVertices(productVertices, authorizations);
            List<String> ids = StreamUtil.stream(productRelatedEdges)
                    .map(edge -> edge.getEdgeId())
                    .collect(Collectors.toList());
            Map<String, Boolean> relatedEdgesById = graph.doEdgesExist(ids, authorizations);

            for (RelatedEdge relatedEdge : productRelatedEdges) {
                String edgeId = relatedEdge.getEdgeId();
                JSONObject edge = new JSONObject();
                edge.put("edgeId", relatedEdge.getEdgeId());

                if (relatedEdgesById.get(edgeId)) {
                    edge.put("label", relatedEdge.getLabel());
                    edge.put("outVertexId", relatedEdge.getOutVertexId());
                    edge.put("inVertexId", relatedEdge.getInVertexId());
                } else {
                    edge.put("unauthorized", true);
                }
                edges.put(edgeId, edge);
            }
            extendedData.put("edges", edges);
        }

        return extendedData;
    }

    @Override
    public void cleanUpElements(Graph graph, Vertex productVertex, Authorizations authorizations) {
        Iterable<Edge> productElementEdges = productVertex.getEdges(
                Direction.OUT,
                WorkspaceProperties.PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                authorizations
        );

        for (Edge productToElement : productElementEdges) {
            graph.softDeleteEdge(productToElement, authorizations);
        }

        graph.flush();
    }

    protected abstract void setEdgeJson(Edge propertyVertexEdge, JSONObject vertex);

    protected abstract void updateProductEdge(ElementUpdateContext<Edge> elemCtx, JSONObject update, Visibility visibility);

    protected String getEdgeId(String productId, String vertexId) {
        return productId + "_hasVertex_" + vertexId;
    }

}
