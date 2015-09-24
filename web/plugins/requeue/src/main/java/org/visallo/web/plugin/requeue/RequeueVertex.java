package org.visallo.web.plugin.requeue;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequeueVertex extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(RequeueVertex.class);
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public RequeueVertex(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration,
            Graph graph,
            WorkQueueRepository workQueueRepository
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String[] vertexIds = getRequiredParameterArray(request, "vertexIds[]");
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        LOGGER.debug("requeuing %d vertices: %s", vertexIds.length, Joiner.on(", ").join(vertexIds));
        Iterable<Vertex> vertices = graph.getVertices(Lists.newArrayList(vertexIds), authorizations);
        requeueVertices(vertices);

        respondWithSuccessJson(response);
    }

    private void requeueVertices(Iterable<Vertex> vertices) {
        for (Vertex vertex : vertices) {
            LOGGER.debug("requeuing vertex: %s", vertex.getId());
            requeueVertex(vertex);
        }
    }

    private void requeueVertex(Vertex vertex) {
        workQueueRepository.pushElement(vertex, Priority.HIGH);
        for (Property property : vertex.getProperties()) {
            workQueueRepository.pushGraphPropertyQueue(vertex, property, Priority.HIGH);
        }
    }
}
