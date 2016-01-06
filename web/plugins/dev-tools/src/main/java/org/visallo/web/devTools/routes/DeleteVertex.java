package org.visallo.web.devTools.routes;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;

public class DeleteVertex implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(DeleteVertex.class);
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public DeleteVertex(
            Graph graph,
            WorkQueueRepository workQueueRepository) {
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "graphVertexId") String graphVertexId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        LOGGER.debug("deleting vertex: %s", graphVertexId);
        long beforeDeletionTimestamp = System.currentTimeMillis() - 1;
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        graph.softDeleteVertex(vertex, authorizations);
        graph.flush();
        LOGGER.info("deleted vertex: %s", graphVertexId);

        this.workQueueRepository.pushVertexDeletion(vertex, beforeDeletionTimestamp, Priority.HIGH);

        return VisalloResponse.SUCCESS;
    }
}
