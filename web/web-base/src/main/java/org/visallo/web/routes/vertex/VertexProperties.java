package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import com.v5analytics.webster.HandlerChain;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexProperties extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public VertexProperties(
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        ClientApiElement element = handle(graphVertexId, workspaceId, authorizations);
        respondWithClientApiObject(response, element);
    }

    private ClientApiElement handle(String graphVertexId, String workspaceId, Authorizations authorizations) {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            return null;
        }
        return ClientApiConverter.toClientApi(vertex, workspaceId, authorizations);
    }
}
