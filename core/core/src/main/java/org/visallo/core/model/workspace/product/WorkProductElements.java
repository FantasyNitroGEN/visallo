package org.visallo.core.model.workspace.product;

import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.*;
import org.visallo.core.model.graph.ElementUpdateContext;
import org.visallo.core.model.graph.GraphUpdateContext;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workspace.WorkspaceProperties;
import org.visallo.core.user.User;
import org.visallo.core.util.JSONUtil;
import org.visallo.core.util.StreamUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WorkProductElements implements WorkProduct, WorkProductHasElements {
    public static final String WORKSPACE_PRODUCT_TO_ENTITY_RELATIONSHIP_IRI = "http://visallo.org/workspace/product#toEntity";

    protected WorkProductElements(OntologyRepository ontologyRepository) {
        addProductToEntityRelationshipToOntology(ontologyRepository);
    }

    private void addProductToEntityRelationshipToOntology(OntologyRepository ontologyRepository) {
        Relationship relationship = ontologyRepository.getRelationshipByIRI(WORKSPACE_PRODUCT_TO_ENTITY_RELATIONSHIP_IRI);
        if (relationship != null) {
            return;
        }

        Concept productConcept = ontologyRepository.getConceptByIRI(WorkspaceProperties.PRODUCT_CONCEPT_IRI);
        checkNotNull(productConcept, "Could not find " + WorkspaceProperties.PRODUCT_CONCEPT_IRI);

        Concept thingConcept = ontologyRepository.getConceptByIRI(VisalloProperties.CONCEPT_TYPE_THING);
        checkNotNull(thingConcept, "Could not find " + VisalloProperties.CONCEPT_TYPE_THING);

        List<Concept> domainConcepts = new ArrayList<>();
        domainConcepts.add(productConcept);
        List<Concept> rangeConcepts = new ArrayList<>();
        rangeConcepts.add(thingConcept);
        ontologyRepository.getOrCreateRelationshipType(
                null,
                domainConcepts,
                rangeConcepts,
                WORKSPACE_PRODUCT_TO_ENTITY_RELATIONSHIP_IRI
        );
        ontologyRepository.clearCache();
    }

    @Override
    public void update(
            GraphUpdateContext ctx,
            Vertex workspaceVertex,
            Vertex productVertex,
            JSONObject params,
            User user,
            Visibility visibility,
            Authorizations authorizations
    ) {
        if (params == null) {
            return;
        }

        JSONObject updateVertices = params.optJSONObject("updateVertices");
        if (updateVertices != null) {
            @SuppressWarnings("unchecked")
            List<String> vertexIds = Lists.newArrayList(updateVertices.keys());
            for (String id : vertexIds) {
                JSONObject updateData = updateVertices.getJSONObject(id);
                String edgeId = getEdgeId(productVertex.getId(), id);
                EdgeBuilderByVertexId edgeBuilder = ctx.getGraph().prepareEdge(
                        edgeId,
                        productVertex.getId(),
                        id,
                        WORKSPACE_PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                        visibility
                );
                ctx.update(edgeBuilder, elemCtx -> updateProductEdge(elemCtx, updateData, visibility));
            }
        }

        JSONArray removeVertices = params.optJSONArray("removeVertices");
        if (removeVertices != null) {
            JSONUtil.toList(removeVertices)
                    .forEach(id -> ctx.getGraph().softDeleteEdge(getEdgeId(productVertex.getId(), (String) id), authorizations));
        }
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
            JSONArray vertices = new JSONArray();
            List<Edge> productVertexEdges = Lists.newArrayList(productVertex.getEdges(
                    Direction.OUT,
                    WORKSPACE_PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
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
                vertices.put(vertex);
            }
            extendedData.put("vertices", vertices);
        }

        if (params.optBoolean("includeEdges")) {
            JSONArray edges = new JSONArray();
            List<String> vertexIds = Lists.newArrayList(productVertex.getVertexIds(
                    Direction.OUT,
                    WORKSPACE_PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                    authorizations
            ));
            Iterable<RelatedEdge> relatedEdges = graph.findRelatedEdgeSummary(vertexIds, authorizations);
            for (RelatedEdge relatedEdge : relatedEdges) {
                JSONObject edge = new JSONObject();
                edge.put("edgeId", relatedEdge.getEdgeId());
                edge.put("label", relatedEdge.getLabel());
                edge.put("outVertexId", relatedEdge.getOutVertexId());
                edge.put("inVertexId", relatedEdge.getInVertexId());
                edges.put(edge);
            }
            extendedData.put("edges", edges);
        }

        return extendedData;
    }

    protected abstract void setEdgeJson(Edge propertyVertexEdge, JSONObject vertex);

    protected abstract void updateProductEdge(ElementUpdateContext<Edge> elemCtx, JSONObject update, Visibility visibility);

    private String getEdgeId(String productId, String vertexId) {
        return productId + "_hasVertex_" + vertexId;
    }

}
