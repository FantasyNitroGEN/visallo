package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiVertexCount;
import org.visallo.web.clientapi.model.ClientApiVertexCountsByConceptType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class VertexGetCount extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public VertexGetCount(
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

        long vertexCount = graph.getVertexCount(authorizations);
        respondWithClientApiObject(response, new ClientApiVertexCount(vertexCount));
    }
}
