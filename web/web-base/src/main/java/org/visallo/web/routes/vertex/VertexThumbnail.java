package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.artifactThumbnails.ArtifactThumbnail;
import org.visallo.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;

import java.io.InputStream;
import java.io.OutputStream;

public class VertexThumbnail implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexThumbnail.class);

    private final ArtifactThumbnailRepository artifactThumbnailRepository;
    private final Graph graph;

    @Inject
    public VertexThumbnail(
            final ArtifactThumbnailRepository artifactThumbnailRepository,
            final Graph graph
    ) {
        this.artifactThumbnailRepository = artifactThumbnailRepository;
        this.graph = graph;
    }

    @Handle
    public void handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Optional(name = "width") Integer width,
            User user,
            Authorizations authorizations,
            VisalloResponse response
    ) throws Exception {
        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            throw new VisalloResourceNotFoundException("Could not find vertex with id: " + graphVertexId);
        }

        int[] boundaryDims = new int[]{200, 200};
        if (width != null) {
            boundaryDims[0] = boundaryDims[1] = width;
        }

        byte[] thumbnailData;
        ArtifactThumbnail thumbnail = artifactThumbnailRepository.getThumbnail(artifactVertex.getId(), "raw", boundaryDims[0], boundaryDims[1], user);
        if (thumbnail != null) {
            String format = thumbnail.getFormat();
            response.setContentType("image/" + format);
            response.addHeader("Content-Disposition", "inline; filename=thumbnail" + boundaryDims[0] + "." + format);
            response.setMaxAge(VisalloResponse.EXPIRES_1_HOUR);

            thumbnailData = thumbnail.getData();
            if (thumbnailData != null) {
                LOGGER.debug("Cache hit for: %s (raw) %d x %d", artifactVertex.getId(), boundaryDims[0], boundaryDims[1]);
                try (OutputStream out = response.getOutputStream()) {
                    out.write(thumbnailData);
                }
                return;
            }
        }

        LOGGER.info("Cache miss for: %s (raw) %d x %d", artifactVertex.getId(), boundaryDims[0], boundaryDims[1]);
        Property rawProperty = VisalloProperties.RAW.getProperty(artifactVertex);
        StreamingPropertyValue rawPropertyValue = VisalloProperties.RAW.getPropertyValue(artifactVertex);
        if (rawPropertyValue == null) {
            throw new VisalloResourceNotFoundException("Could not find raw property on vertex: " + artifactVertex.getId());
        }

        try (InputStream in = rawPropertyValue.getInputStream()) {
            thumbnail = artifactThumbnailRepository.createThumbnail(artifactVertex, rawProperty.getKey(), "raw", in, boundaryDims, user);

            String format = thumbnail.getFormat();
            response.setContentType("image/" + format);
            response.addHeader("Content-Disposition", "inline; filename=thumbnail" + boundaryDims[0] + "." + format);
            response.setMaxAge(VisalloResponse.EXPIRES_1_HOUR);

            thumbnailData = thumbnail.getData();
        }
        try (OutputStream out = response.getOutputStream()) {
            out.write(thumbnailData);
        }
    }
}
