package org.visallo.web.plugin.requeue;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;

public class RequeueVertex implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(RequeueVertex.class);
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public RequeueVertex(
            Graph graph,
            WorkQueueRepository workQueueRepository
    ) {
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "vertexIds[]") String[] vertexIdsParam,
            Authorizations authorizations
    ) throws Exception {
        LOGGER.debug("requeuing %d vertices: %s", vertexIdsParam.length, Joiner.on(", ").join(vertexIdsParam));
        Iterable<Vertex> vertices = graph.getVertices(Lists.newArrayList(vertexIdsParam), authorizations);
        requeueVertices(vertices);

        return VisalloResponse.SUCCESS;
    }

    private void requeueVertices(Iterable<Vertex> vertices) {
        for (Vertex vertex : vertices) {
            requeueVertex(vertex);
        }
    }

    private void requeueVertex(Vertex vertex) {
        LOGGER.debug("requeuing vertex: %s", vertex.getId());
        workQueueRepository.pushElement(vertex, Priority.HIGH);
    }
}
