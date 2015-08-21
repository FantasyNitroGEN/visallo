package org.visallo.web.devTools.routes;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DeleteVertex extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(DeleteVertex.class);
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public DeleteVertex(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration,
            Graph graph,
            WorkQueueRepository workQueueRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        LOGGER.debug("deleting vertex: %s", graphVertexId);
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        graph.softDeleteVertex(vertex, authorizations);
        graph.flush();
        LOGGER.info("deleted vertex: %s", graphVertexId);

        this.workQueueRepository.pushVertexDeletion(vertex);

        respondWithHtml(response, "Deleted vertex");
    }
}
