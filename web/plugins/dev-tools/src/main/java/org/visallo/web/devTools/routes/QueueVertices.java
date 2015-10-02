package org.visallo.web.devTools.routes;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;

public class QueueVertices implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(QueueVertices.class);
    private final UserRepository userRepository;
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public QueueVertices(
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
            @Optional(name = "propertyName") String propertyName
    ) throws Exception {
        if (propertyName != null && propertyName.trim().length() == 0) {
            propertyName = null;
        }
        final Authorizations authorizations = userRepository.getAuthorizations(userRepository.getSystemUser());

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
                        workQueueRepository.pushElement(vertex, Priority.NORMAL);
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

        return VisalloResponse.SUCCESS;
    }
}
