package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import com.v5analytics.webster.HandlerChain;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiEdgesExistsResponse;
import org.vertexium.Authorizations;
import org.vertexium.Graph;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EdgeExists extends BaseRequestHandler {
    private final Graph graph;

    @Inject
    public EdgeExists(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        List<String> edgeIds = Arrays.asList(getRequiredParameterArray(request, "edgeIds[]"));
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Map<String, Boolean> graphEdiges = graph.doEdgesExist(edgeIds, authorizations);
        ClientApiEdgesExistsResponse result = new ClientApiEdgesExistsResponse();
        result.getExists().putAll(graphEdiges);

        respondWithClientApiObject(response, result);
    }
}
