package org.visallo.web.devTools;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import com.v5analytics.webster.HandlerChain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class QueueEdges extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(QueueEdges.class);
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public QueueEdges(
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
        String label = getOptionalParameter(request, "label");
        if (label != null && label.trim().length() == 0) {
            label = null;
        }
        final Authorizations authorizations = getUserRepository().getAuthorizations(getUserRepository().getSystemUser());

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
                    workQueueRepository.pushElement(edge);
                }
                workQueueRepository.flush();
                LOGGER.info("requeue all edges complete");
            }
        });
        t.setName("requeue-edges");
        t.start();

        respondWithHtml(response, "Started requeue thread");
    }
}
