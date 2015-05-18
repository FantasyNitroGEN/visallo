package org.visallo.core.model.artifactThumbnails;

import com.google.inject.Inject;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.types.BooleanVisalloProperty;
import org.visallo.core.model.properties.types.IntegerVisalloProperty;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.vertexium.Vertex;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public class ArtifactThumbnailRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ArtifactThumbnailRepository.class);
    private static final String VISIBILITY_STRING = "";
    public static int FRAMES_PER_PREVIEW = 20;
    public static int PREVIEW_FRAME_WIDTH = 360;
    public static int PREVIEW_FRAME_HEIGHT = 240;
    private final SimpleOrmSession simpleOrmSession;
    private BooleanVisalloProperty yAxisFlippedProperty;
    private IntegerVisalloProperty clockwiseRotationProperty;

    @Inject
    public ArtifactThumbnailRepository(
            SimpleOrmSession simpleOrmSession,
            final OntologyRepository ontologyRepository
    ) {
        this.simpleOrmSession = simpleOrmSession;

        String yAxisFlippedPropertyIri = ontologyRepository.getPropertyIRIByIntent("media.yAxisFlipped");
        if (yAxisFlippedPropertyIri != null) {
            this.yAxisFlippedProperty = new BooleanVisalloProperty(yAxisFlippedPropertyIri);
        }

        String clockwiseRotationPropertyIri = ontologyRepository.getPropertyIRIByIntent("media.clockwiseRotation");
        if (clockwiseRotationPropertyIri != null) {
            this.clockwiseRotationProperty = new IntegerVisalloProperty(clockwiseRotationPropertyIri);
        }
    }

    public ArtifactThumbnail getThumbnail(String artifactVertexId, String thumbnailType, int width, int height, User user) {
        String id = ArtifactThumbnail.createId(artifactVertexId, thumbnailType, width, height);
        return simpleOrmSession.findById(ArtifactThumbnail.class, id, user.getSimpleOrmContext());
    }

    public byte[] getThumbnailData(String artifactVertexId, String thumbnailType, int width, int height, User user) {
        ArtifactThumbnail artifactThumbnail = getThumbnail(artifactVertexId, thumbnailType, width, height, user);
        if (artifactThumbnail == null) {
            return null;
        }
        return artifactThumbnail.getData();
    }

    public ArtifactThumbnail createThumbnail(Vertex artifactVertex, String propertyKey, String thumbnailType, InputStream in, int[] boundaryDims, User user) throws IOException {
        ArtifactThumbnail thumbnail = generateThumbnail(artifactVertex, propertyKey, thumbnailType, in, boundaryDims);
        simpleOrmSession.save(thumbnail, VISIBILITY_STRING, user.getSimpleOrmContext());
        return thumbnail;
    }

    public ArtifactThumbnail generateThumbnail(Vertex artifactVertex, String propertyKey, String thumbnailType, InputStream in, int[] boundaryDims) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String format;
        int type;
        try {
            BufferedImage originalImage = ImageIO.read(in);
            checkNotNull(originalImage, "Could not generateThumbnail: read original image for artifact " + artifactVertex.getId());
            type = ImageUtils.thumbnailType(originalImage);
            format = ImageUtils.thumbnailFormat(originalImage);

            BufferedImage transformedImage = getTransformedImage(originalImage, artifactVertex, propertyKey);

            //Get new image dimensions, which will be used for the icon.
            int[] transformedImageDims = new int[]{transformedImage.getWidth(), transformedImage.getHeight()};
            int[] newImageDims = getScaledDimension(transformedImageDims, boundaryDims);
            if (newImageDims[0] >= transformedImageDims[0] || newImageDims[1] >= transformedImageDims[1]) {
                LOGGER.info("Original image dimensions %d x %d are smaller "
                                + "than requested dimensions %d x %d returning original.",
                        transformedImageDims[0], transformedImageDims[1],
                        newImageDims[0], newImageDims[1]);
            }
            //Resize the image.
            BufferedImage resizedImage = new BufferedImage(newImageDims[0], newImageDims[1], type);
            Graphics2D g = resizedImage.createGraphics();
            int width = resizedImage.getWidth();
            int height = resizedImage.getHeight();
            if (transformedImage.getColorModel().getNumComponents() > 3) {
                g.drawImage(transformedImage, 0, 0, width, height, null);
            } else {
                g.drawImage(transformedImage, 0, 0, width, height, Color.BLACK, null);
            }
            g.dispose();

            //Write the bufferedImage to a file.
            ImageIO.write(resizedImage, format, out);

            return new ArtifactThumbnail(artifactVertex.getId(), thumbnailType, out.toByteArray(), format, width, height);
        } catch (IOException e) {
            throw new VisalloResourceNotFoundException("Error reading InputStream");
        }
    }

    public BufferedImage getTransformedImage(BufferedImage originalImage, Vertex artifactVertex, String propertyKey) {
        int cwRotationNeeded = 0;
        if (clockwiseRotationProperty != null) {
            Integer nullable = clockwiseRotationProperty.getPropertyValue(artifactVertex, propertyKey);
            if (nullable != null) {
                cwRotationNeeded = nullable;
            }
        }
        boolean yAxisFlipNeeded = false;
        if (yAxisFlippedProperty != null) {
            Boolean nullable = yAxisFlippedProperty.getPropertyValue(artifactVertex, propertyKey);
            if (nullable != null) {
                yAxisFlipNeeded = nullable;
            }
        }

        //Rotate and flip image.
        return ImageUtils.reOrientImage(originalImage, yAxisFlipNeeded, cwRotationNeeded);
    }

    public int[] getScaledDimension(int[] imgSize, int[] boundary) {
        int originalWidth = imgSize[0];
        int originalHeight = imgSize[1];
        int boundWidth = boundary[0];
        int boundHeight = boundary[1];
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        if (originalWidth > boundWidth) {
            newWidth = boundWidth;
            newHeight = (newWidth * originalHeight) / originalWidth;
        }

        if (newHeight > boundHeight) {
            newHeight = boundHeight;
            newWidth = (newHeight * originalWidth) / originalHeight;
        }

        return new int[]{newWidth, newHeight};
    }
}
