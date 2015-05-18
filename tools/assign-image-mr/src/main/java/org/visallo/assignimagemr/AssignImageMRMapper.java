package org.visallo.assignimagemr;

import org.visallo.vertexium.mapreduce.VisalloElementMapperBase;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.vertexium.Direction;
import org.vertexium.Element;
import org.vertexium.Vertex;
import org.vertexium.VertexBuilder;

import java.io.IOException;

import static com.google.common.collect.Iterables.isEmpty;

public class AssignImageMRMapper extends VisalloElementMapperBase<Text, Element> {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(AssignImageMRMapper.class);
    private Counter elementsProcessedCounter;
    private Counter assignmentsMadeCounter;
    private AssignImageConfiguration config;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        this.elementsProcessedCounter = context.getCounter(AssignImageCounters.ELEMENTS_PROCESSED);
        this.assignmentsMadeCounter = context.getCounter(AssignImageCounters.ASSIGNMENTS_MADE);
        this.config = new AssignImageConfiguration(context.getConfiguration());
    }

    @Override
    protected void safeMap(Text key, Element element, Context context) throws Exception {
        Vertex vertex = (Vertex) element;
        context.setStatus("Processing " + vertex.getId());

        if (isEmpty(vertex.getProperties(VisalloProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName()))
                && isEmpty(vertex.getProperties(VisalloProperties.ENTITY_IMAGE_URL.getPropertyName()))) {
            LOGGER.debug("no image on vertex '%s', finding best image.", vertex.getId());
            String imageVertexId = findBestImageVertexId(vertex);
            if (imageVertexId == null) {
                LOGGER.debug("no image found for vertex '%s'", vertex.getId());
            } else {
                LOGGER.debug("image '%s' found for vertex '%s'. assigning.", imageVertexId, vertex.getId());
                VertexBuilder m = prepareVertex(vertex.getId(), vertex.getVisibility());
                VisalloProperties.ENTITY_IMAGE_VERTEX_ID.setProperty(m, imageVertexId, this.config.getVisibility());
                m.save(this.config.getAuthorizations());
                assignmentsMadeCounter.increment(1);
            }
        }

        elementsProcessedCounter.increment(1);
    }

    private String findBestImageVertexId(Vertex vertex) {
        Iterable<Vertex> vertices = vertex.getVertices(Direction.OUT, config.getHasImageLabels(), config.getAuthorizations());
        for (Vertex v : vertices) {
            String mimeType = VisalloProperties.MIME_TYPE.getOnlyPropertyValue(v);
            if (mimeType != null && mimeType.startsWith("image")) {
                return v.getId();
            }
        }
        return null;
    }
}
