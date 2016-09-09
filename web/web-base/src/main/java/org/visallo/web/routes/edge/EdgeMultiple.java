package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.FetchHint;
import org.vertexium.Graph;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiEdge;
import org.visallo.web.clientapi.model.ClientApiEdgeMultipleResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.AuthorizationsParameterProviderFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.vertexium.util.IterableUtils.toIterable;
import static org.vertexium.util.IterableUtils.toList;

public class EdgeMultiple implements ParameterizedHandler {
    private final Graph graph;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public EdgeMultiple(
            Graph graph,
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            AuthorizationRepository authorizationRepository
    ) {
        this.graph = graph;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.authorizationRepository = authorizationRepository;
    }

    @Handle
    public ClientApiEdgeMultipleResponse handle(
            @Required(name = "edgeIds[]") String[] edgeIdsParameter,
            @ActiveWorkspaceId(required = false) String workspaceId,
            HttpServletRequest request,
            User user
    ) throws Exception {
        HashSet<String> edgeStringIds = new HashSet<>(Arrays.asList(edgeIdsParameter));

        Authorizations authorizations = getAuthorizations(request, false, user);

        Iterable<String> edgeIds = toIterable(edgeStringIds.toArray(new String[edgeStringIds.size()]));
        return getEdges(request, workspaceId, edgeIds, authorizations);
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
        List<Edge> graphEdges = toList(graph.getEdges(edgeIds, FetchHint.ALL, authorizations));
        ClientApiEdgeMultipleResponse edgeResult = new ClientApiEdgeMultipleResponse();
        for (Edge e : graphEdges) {
            ClientApiEdge clientApiEdge = ClientApiConverter.toClientApiEdge(e, workspaceId);
            edgeResult.getEdges().add(clientApiEdge);
        }
        return edgeResult;
    }

    private Authorizations getAuthorizations(HttpServletRequest request, boolean fallbackToPublic, User user) {
        GetAuthorizationsResult result = new GetAuthorizationsResult();
        result.requiredFallback = false;
        try {
            return AuthorizationsParameterProviderFactory.getAuthorizations(
                    request,
                    userRepository,
                    authorizationRepository,
                    workspaceRepository
            );
        } catch (VisalloAccessDeniedException ex) {
            if (fallbackToPublic) {
                return authorizationRepository.getGraphAuthorizations(user);
            } else {
                throw ex;
            }
        }
    }

    private static class GetAuthorizationsResult {
        public Authorizations authorizations;
        public boolean requiredFallback;
    }
}
