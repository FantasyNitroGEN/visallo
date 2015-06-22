package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiVertexCount;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EdgeGetCount extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public EdgeGetCount(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration,
            Graph graph
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        long vertexCount = graph.getEdgeCount(authorizations);
        respondWithClientApiObject(response, new ClientApiVertexCount(vertexCount));
    }
}
