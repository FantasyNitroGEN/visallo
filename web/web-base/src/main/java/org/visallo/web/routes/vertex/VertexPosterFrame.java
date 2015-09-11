package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.apache.commons.io.IOUtils;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import org.visallo.core.model.properties.MediaVisalloProperties;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;

import java.io.InputStream;
import java.io.OutputStream;

public class VertexPosterFrame implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexPosterFrame.class);
    private final Graph graph;
    private final ArtifactThumbnailRepository artifactThumbnailRepository;

    @Inject
    public VertexPosterFrame(
            final Graph graph,
            final ArtifactThumbnailRepository artifactThumbnailRepository
    ) {
        this.graph = graph;
        this.artifactThumbnailRepository = artifactThumbnailRepository;
    }

    @Handle
    public void handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Optional(name = "width") Integer width,
            VisalloResponse response,
            Authorizations authorizations,
            User user
    ) throws Exception {
        int[] boundaryDims = new int[]{200, 200};

        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            response.respondWithNotFound();
            return;
        }

        if (width != null) {
            boundaryDims[0] = boundaryDims[1] = width;

            response.setContentType("image/jpeg");
            response.addHeader("Content-Disposition", "inline; filename=thumbnail" + boundaryDims[0] + ".jpg");
            response.setMaxAge(VisalloResponse.EXPIRES_1_HOUR);

            byte[] thumbnailData = artifactThumbnailRepository.getThumbnailData(artifactVertex.getId(), "poster-frame", boundaryDims[0], boundaryDims[1], user);
            if (thumbnailData != null) {
                LOGGER.debug("Cache hit for: %s (poster-frame) %d x %d", graphVertexId, boundaryDims[0], boundaryDims[1]);
                OutputStream out = response.getOutputStream();
                out.write(thumbnailData);
                out.close();
                return;
            }
        }

        Property rawPosterFrame = MediaVisalloProperties.RAW_POSTER_FRAME.getOnlyProperty(artifactVertex);
        StreamingPropertyValue rawPosterFrameValue = MediaVisalloProperties.RAW_POSTER_FRAME.getPropertyValue(rawPosterFrame);
        if (rawPosterFrameValue == null) {
            LOGGER.warn("Could not find raw poster from for artifact: %s", artifactVertex.getId());
            response.respondWithNotFound();
            return;
        }

        try (InputStream in = rawPosterFrameValue.getInputStream()) {
            if (width != null) {
                LOGGER.info("Cache miss for: %s (poster-frame) %d x %d", graphVertexId, boundaryDims[0], boundaryDims[1]);

                response.setContentType("image/jpeg");
                response.addHeader("Content-Disposition", "inline; filename=thumbnail" + boundaryDims[0] + ".jpg");
                response.setMaxAge(VisalloResponse.EXPIRES_1_HOUR);

                byte[] thumbnailData = artifactThumbnailRepository.createThumbnail(artifactVertex, rawPosterFrame.getKey(), "poster-frame", in, boundaryDims, user).getData();
                OutputStream out = response.getOutputStream();
                out.write(thumbnailData);
                out.close();
            } else {
                response.setContentType("image/png");
                IOUtils.copy(in, response.getOutputStream());
            }
        }
    }
}
