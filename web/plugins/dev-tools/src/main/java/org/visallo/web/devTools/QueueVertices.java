package org.visallo.web.devTools;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import com.v5analytics.webster.HandlerChain;
import org.visallo.web.BaseRequestHandler;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class QueueVertices extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(QueueVertices.class);
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public QueueVertices(
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
        String propertyName = getOptionalParameter(request, "propertyName");
        if (propertyName != null && propertyName.trim().length() == 0) {
            propertyName = null;
        }
        final Authorizations authorizations = getUserRepository().getAuthorizations(getUserRepository().getSystemUser());

        final String finalPropertyName = propertyName;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("requeue all vertices (property: %s)", finalPropertyName);
                int count = 0;
                int pushedCount = 0;
                Iterable<Vertex> vertices = graph.getVertices(authorizations);
                for (Vertex vertex : vertices) {
                    if (finalPropertyName == null) {
                        workQueueRepository.broadcastElement(vertex, null);
                        pushedCount++;
                    } else {
                        Iterable<Property> properties = vertex.getProperties(finalPropertyName);
                        for (Property property : properties) {
                            workQueueRepository.pushGraphPropertyQueue(vertex, property, Priority.NORMAL);
                            pushedCount++;
                        }
                    }
                    count++;
                    if ((count % 10000) == 0) {
                        LOGGER.debug("requeue status. vertices looked at %d. items pushed %d. last vertex id: %s", count, pushedCount, vertex.getId());
                    }
                }
                workQueueRepository.flush();
                LOGGER.info("requeue all vertices complete. vertices looked at %d. items pushed %d.", count, pushedCount);
            }
        });
        t.setName("requeue-vertices");
        t.start();

        respondWithHtml(response, "Started requeue thread");
    }
}
