package org.visallo.tools;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.cmdline.converters.WorkQueuePriorityConverter;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

@Parameters(commandDescription = "Queues elements or element properties onto the GPW queue")
public class Requeue extends CommandLineTool {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(Requeue.class, "cli-requeue");

    @Parameter(names = {"--vertexid"}, arity = 1, description = "The Vertex id to requeue")
    private String vertexId;

    @Parameter(names = {"--propertyname", "-pn"}, arity = 1, description = "The name of the property to requeue")
    private String propertyName;

    @Parameter(names = {"--priority", "-p"}, arity = 1, converter = WorkQueuePriorityConverter.class, description = "Priority at which to enqueue")
    private Priority priority = Priority.LOW;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new Requeue(), args);
    }

    @Override
    protected int run() throws Exception {
        System.out.println("requeue all vertices (property: " + propertyName + ")");
        LOGGER.info("requeue all vertices (property: %s)", propertyName);
        AtomicInteger pushedCount = new AtomicInteger();
        AtomicInteger count = new AtomicInteger();
        if (vertexId == null) {
            pushVertices(count, pushedCount);
        } else {
            Vertex vertex = getGraph().getVertex(vertexId, getAuthorizations());
            if (vertex == null) {
                throw new VisalloException("Could not find vertex: " + vertexId);
            }
            LOGGER.info("requeueing vertex: %s", vertex.getId());
            pushVertex(vertex, pushedCount);
        }
        getWorkQueueRepository().flush();
        LOGGER.info("requeue all vertices complete. vertices looked at %d. items pushed %d.", count.get(), pushedCount.get());

        return 0;
    }

    private void pushVertices(AtomicInteger count, AtomicInteger pushedCount) {
        Iterable<Vertex> vertices = getGraph().getVertices(getAuthorizations());
        for (Vertex vertex : vertices) {
            pushVertex(vertex, pushedCount);
            int countValue = count.incrementAndGet();
            if ((countValue % 10000) == 0) {
                LOGGER.debug("requeue status. vertices looked at %d. items pushed %d. last vertex id: %s", countValue, pushedCount.get(), vertex.getId());
            }
        }
    }

    private void pushVertex(Vertex vertex, AtomicInteger pushedCount) {
        if (propertyName == null) {
            getWorkQueueRepository().pushElement(vertex, priority);
            pushedCount.incrementAndGet();
        } else {
            Iterable<Property> properties = vertex.getProperties(propertyName);
            for (Property property : properties) {
                getWorkQueueRepository().pushGraphPropertyQueue(vertex, property, priority);
                pushedCount.incrementAndGet();
            }
        }
    }
}
