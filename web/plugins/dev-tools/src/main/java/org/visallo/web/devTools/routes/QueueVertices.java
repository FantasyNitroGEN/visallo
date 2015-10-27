package org.visallo.web.devTools.routes;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.query.Compare;
import org.vertexium.query.Query;
import org.visallo.core.model.properties.VisalloProperties;
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
            @Required(name = "priority") String priorityString,
            @Optional(name = "conceptType") String conceptType,
            @Optional(name = "propertyName") String propertyName
    ) throws Exception {
        final Priority priority = Priority.safeParse(priorityString);
        if (conceptType != null && conceptType.trim().length() == 0) {
            conceptType = null;
        }
        if (propertyName != null && propertyName.trim().length() == 0) {
            propertyName = null;
        }
        final Authorizations authorizations = userRepository.getAuthorizations(userRepository.getSystemUser());

        final String finalConceptType = conceptType;
        final String finalPropertyName = propertyName;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("requeue vertices (with concept type: %s, property name: %s)", finalConceptType, finalPropertyName);
                int count = 0;
                int pushedCount = 0;
                Query q = graph.query(authorizations);
                if (finalConceptType != null) {
                    q = q.has(VisalloProperties.CONCEPT_TYPE.getPropertyName(), Compare.EQUAL, finalConceptType);
                }
                if (finalPropertyName != null) {
                    q = q.has(finalPropertyName);
                }
                for (Vertex vertex : q.vertices()) {
                    if (finalPropertyName == null) {
                        workQueueRepository.pushElement(vertex, priority);
                        pushedCount++;
                    } else {
                        Iterable<Property> properties = vertex.getProperties(finalPropertyName);
                        for (Property property : properties) {
                            workQueueRepository.pushGraphPropertyQueue(vertex, property, priority);
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
