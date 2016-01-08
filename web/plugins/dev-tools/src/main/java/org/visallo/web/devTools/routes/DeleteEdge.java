package org.visallo.web.devTools.routes;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;

public class DeleteEdge implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(DeleteEdge.class);
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public DeleteEdge(
            Graph graph,
            WorkQueueRepository workQueueRepository
    ) {
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "edgeId") String edgeId,
            Authorizations authorizations
    ) throws Exception {
        LOGGER.debug("deleting edge: %s", edgeId);
        long beforeDeletionTimestamp = System.currentTimeMillis() - 1;
        Edge edge = graph.getEdge(edgeId, authorizations);
        graph.softDeleteEdge(edge, authorizations);
        graph.flush();
        LOGGER.info("deleted edge: %s", edgeId);

        this.workQueueRepository.pushEdgeDeletion(edge, beforeDeletionTimestamp, Priority.HIGH);

        return VisalloResponse.SUCCESS;
    }
}
