package org.visallo.web.routes.vertex;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.*;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiElementFindRelatedResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;

public class VertexFindRelated implements ParameterizedHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;

    @Inject
    public VertexFindRelated(
            final OntologyRepository ontologyRepository,
            final Graph graph
    ) {
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
    }

    @Handle
    public ClientApiElementFindRelatedResponse handle(
            @Required(name = "graphVertexIds[]") String[] graphVertexIds,
            @Optional(name = "limitParentConceptId") String limitParentConceptId,
            @Optional(name = "limitEdgeLabel") String limitEdgeLabel,
            @Optional(name = "maxVerticesToReturn", defaultValue = "250") long maxVerticesToReturn,
            HttpServletRequest request,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        Set<String> limitConceptIds = new HashSet<>();

        if (limitParentConceptId != null) {
            Set<Concept> limitConcepts = ontologyRepository.getConceptAndAllChildrenByIri(limitParentConceptId);
            if (limitConcepts == null) {
                throw new RuntimeException("Bad 'limitParentConceptId', no concept found for id: " +
                        limitParentConceptId);
            }
            for (Concept con : limitConcepts) {
                limitConceptIds.add(con.getIRI());
            }
        }

        return getVertices(request, workspaceId, graphVertexIds, limitEdgeLabel, limitConceptIds, maxVerticesToReturn, authorizations);
    }

    /**
     * This is overridable so web plugins can modify the resulting set of vertices.
     */
    protected ClientApiElementFindRelatedResponse getVertices(HttpServletRequest request, String workspaceId,
                                                              String[] graphVertexIds, String limitEdgeLabel,
                                                              Set<String> limitConceptIds, long maxVerticesToReturn,
                                                              Authorizations authorizations) {
        Set<String> visitedIds = new HashSet<>();
        ClientApiElementFindRelatedResponse vertexResult = new ClientApiElementFindRelatedResponse();
        long count = visitedIds.size();
        Iterable<Vertex> vertices = graph.getVertices(Lists.newArrayList(graphVertexIds), FetchHint.EDGE_REFS, authorizations);
        for (Vertex v : vertices) {
            Iterable<Vertex> relatedVertices = v.getVertices(Direction.BOTH, limitEdgeLabel, ClientApiConverter.SEARCH_FETCH_HINTS, authorizations);
            for (Vertex vertex : relatedVertices) {
                if (!visitedIds.add(vertex.getId())) {
                    continue;
                }
                if (limitConceptIds.size() == 0 || !isLimited(vertex, limitConceptIds)) {
                    if (count < maxVerticesToReturn) {
                        vertexResult.getElements().add(ClientApiConverter.toClientApiVertex(vertex, workspaceId, authorizations));
                    }
                    count++;
                }
            }
        }
        vertexResult.setCount(count);
        return vertexResult;
    }

    private boolean isLimited(Vertex vertex, Set<String> limitConceptIds) {
        String conceptId = VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex);
        return !limitConceptIds.contains(conceptId);
    }
}

