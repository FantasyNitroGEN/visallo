package org.visallo.web.routes.edge;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.FetchHint;
import org.vertexium.Graph;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiEdge;
import org.visallo.web.clientapi.model.ClientApiEdgeMultipleResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import javax.servlet.http.HttpServletRequest;

public class EdgeMultiple implements ParameterizedHandler {
    private final Graph graph;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public EdgeMultiple(
            Graph graph,
            AuthorizationRepository authorizationRepository
    ) {
        this.graph = graph;
        this.authorizationRepository = authorizationRepository;
    }

    @Handle
    public ClientApiEdgeMultipleResponse handle(
            @Required(name = "edgeIds[]") String[] edgeIdsParameter,
            @ActiveWorkspaceId(required = false) String workspaceId,
            HttpServletRequest request,
            User user
    ) throws Exception {
        Authorizations authorizations = workspaceId != null ?
                authorizationRepository.getGraphAuthorizations(user, workspaceId) :
                authorizationRepository.getGraphAuthorizations(user);

        return getEdges(request, workspaceId, Sets.newHashSet(edgeIdsParameter), authorizations);
    }

    /**
     * This is overridable so web plugins can modify the resulting set of edges.
     */
    @SuppressWarnings("UnusedParameters")
    protected ClientApiEdgeMultipleResponse getEdges(
            HttpServletRequest request,
            String workspaceId,
            Iterable<String> edgeIds,
            Authorizations authorizations
    ) {
        ClientApiEdgeMultipleResponse edgeResult = new ClientApiEdgeMultipleResponse();

        Iterable<Edge> edges = graph.getEdges(edgeIds, FetchHint.ALL, authorizations);
        for (Edge e : edges) {
            ClientApiEdge clientApiEdge = ClientApiConverter.toClientApiEdge(e, workspaceId);
            edgeResult.getEdges().add(clientApiEdge);
        }

        return edgeResult;
    }
}
