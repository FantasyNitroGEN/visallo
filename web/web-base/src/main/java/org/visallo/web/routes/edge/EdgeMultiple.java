package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import com.v5analytics.webster.HandlerChain;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiEdgeMultipleResponse;
import org.visallo.web.clientapi.model.ClientApiEdgeWithVertexData;
import org.vertexium.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;

import static org.vertexium.util.IterableUtils.toIterable;

public class EdgeMultiple extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public EdgeMultiple(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        HashSet<String> edgeStringIds = new HashSet<>(Arrays.asList(getRequiredParameterArray(request, "edgeIds[]")));

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, false, user).authorizations;
        String workspaceId = getWorkspaceId(request);

        Iterable<String> edgeIds = toIterable(edgeStringIds.toArray(new String[edgeStringIds.size()]));
        ClientApiEdgeMultipleResponse result = getEdges(request, workspaceId, edgeIds, authorizations);

        respondWithClientApiObject(response, result);
    }

    /**
     * This is overridable so web plugins can modify the resulting set of edges.
     */
    protected ClientApiEdgeMultipleResponse getEdges(HttpServletRequest request, String workspaceId,
                                                   Iterable<String> edgeIds, Authorizations authorizations) {
        Iterable<Edge> graphEdges = graph.getEdges(edgeIds, FetchHint.ALL, authorizations);
        ClientApiEdgeMultipleResponse edgeResult = new ClientApiEdgeMultipleResponse();
        for (Edge e : graphEdges) {
            Vertex source = e.getVertex(Direction.OUT, authorizations);
            Vertex destination = e.getVertex(Direction.IN, authorizations);
            edgeResult.getEdges().add((ClientApiEdgeWithVertexData) ClientApiConverter.toClientApiEdgeWithVertexData(
                    e, source, destination, workspaceId, authorizations));
        }
        return edgeResult;
    }

    private GetAuthorizationsResult getAuthorizations(HttpServletRequest request, boolean fallbackToPublic, User user) {
        GetAuthorizationsResult result = new GetAuthorizationsResult();
        result.requiredFallback = false;
        try {
            result.authorizations = getAuthorizations(request, user);
        } catch (VisalloAccessDeniedException ex) {
            if (fallbackToPublic) {
                result.authorizations = getUserRepository().getAuthorizations(user);
                result.requiredFallback = true;
            } else {
                throw ex;
            }
        }
        return result;
    }

    private String getWorkspaceId(HttpServletRequest request) {
        String workspaceId;
        try {
            workspaceId = getActiveWorkspaceId(request);
        } catch (VisalloException ex) {
            workspaceId = null;
        }
        return workspaceId;
    }

    private static class GetAuthorizationsResult {
        public Authorizations authorizations;
        public boolean requiredFallback;
    }
}
