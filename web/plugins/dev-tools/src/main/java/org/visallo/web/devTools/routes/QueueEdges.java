package org.visallo.web.devTools.routes;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;

public class QueueEdges implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(QueueEdges.class);
    private final UserRepository userRepository;
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public QueueEdges(
            UserRepository userRepository,
            Graph graph,
            WorkQueueRepository workQueueRepository
    ) {
        this.userRepository = userRepository;
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Optional(name = "label") String label
    ) throws Exception {
        if (label != null && label.trim().length() == 0) {
            label = null;
        }
        final Authorizations authorizations = userRepository.getAuthorizations(userRepository.getSystemUser());

        final String finalLabel = label;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("requeue all edges");
                Iterable<Edge> edges = graph.getEdges(authorizations);
                for (Edge edge : edges) {
                    if (finalLabel != null && !finalLabel.equals(edge.getLabel())) {
                        continue;
                    }
                    workQueueRepository.broadcastElement(edge, null);
                }
                workQueueRepository.flush();
                LOGGER.info("requeue all edges complete");
            }
        });
        t.setName("requeue-edges");
        t.start();

        return VisalloResponse.SUCCESS;
    }
}
