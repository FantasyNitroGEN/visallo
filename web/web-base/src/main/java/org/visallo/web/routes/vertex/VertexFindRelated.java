package org.visallo.web.routes.vertex;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.*;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiVertexFindRelatedResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Set;

public class VertexFindRelated extends BaseRequestHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;

    @Inject
    public VertexFindRelated(
            final OntologyRepository ontologyRepository,
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String[] graphVertexIds = getRequiredParameterArray(request, "graphVertexIds[]");
        String limitParentConceptId = getOptionalParameter(request, "limitParentConceptId");
        String limitEdgeLabel = getOptionalParameter(request, "limitEdgeLabel");
        long maxVerticesToReturn = getOptionalParameterLong(request, "maxVerticesToReturn", 250);

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

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

        ClientApiVertexFindRelatedResponse result = getVertices(request, workspaceId, graphVertexIds, limitEdgeLabel,
                limitConceptIds, maxVerticesToReturn, authorizations);

        respondWithClientApiObject(response, result);
    }

    /**
     * This is overridable so web plugins can modify the resulting set of vertices.
     */
    protected ClientApiVertexFindRelatedResponse getVertices(HttpServletRequest request, String workspaceId,
                                                             String[] graphVertexIds, String limitEdgeLabel,
                                                             Set<String> limitConceptIds, long maxVerticesToReturn,
                                                             Authorizations authorizations) {
        Set<String> visitedIds = new HashSet<>();
        ClientApiVertexFindRelatedResponse vertexResult = new ClientApiVertexFindRelatedResponse();
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
                        vertexResult.getVertices().add(ClientApiConverter.toClientApiVertex(vertex, workspaceId, authorizations));
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

