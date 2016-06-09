package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.*;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VertexiumUtil;
import org.visallo.web.clientapi.model.ClientApiEdgeMultipleResponse;
import org.visallo.web.clientapi.model.ClientApiEdgeWithVertexData;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.AuthorizationsParameterProviderFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

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
    protected ClientApiEdgeMultipleResponse getEdges(
            HttpServletRequest request,
            String workspaceId,
            Iterable<String> edgeIds,
            Authorizations authorizations
    ) {
        List<Edge> graphEdges = toList(graph.getEdges(edgeIds, FetchHint.ALL, authorizations));
        ClientApiEdgeMultipleResponse edgeResult = new ClientApiEdgeMultipleResponse();
        Set<String> vertexIds = VertexiumUtil.getAllVertexIdsOnEdges(graphEdges);
        Map<String, Vertex> vertices = VertexiumUtil.verticesToMapById(graph.getVertices(vertexIds, authorizations));
        for (Edge e : graphEdges) {
            Vertex source = vertices.get(e.getVertexId(Direction.OUT));
            Vertex destination = vertices.get(e.getVertexId(Direction.IN));
            ClientApiEdgeWithVertexData clientApiEdgeWithVertexData = ClientApiConverter.toClientApiEdgeWithVertexData(
                    e,
                    source,
                    destination,
                    workspaceId,
                    authorizations
            );
            edgeResult.getEdges().add(clientApiEdgeWithVertexData);
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
