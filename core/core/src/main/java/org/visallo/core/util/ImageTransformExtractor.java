package org.visallo.core.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

import java.io.*;

public class ImageTransformExtractor {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ImageTransformExtractor.class);

    public static ImageTransform getImageTransform(byte[] data) {
        return getImageTransform(new ByteArrayInputStream(data));
    }

    public static ImageTransform getImageTransform(InputStream inputStream) {
        try {
            //Attempt to retrieve the metadata from the image.
            BufferedInputStream in = new BufferedInputStream(inputStream);
            Metadata metadata = ImageMetadataReader.readMetadata(in);
            return getImageTransformFromMetadata(metadata);
        } catch (ImageProcessingException e) {
            LOGGER.error("drewnoakes metadata extractor threw ImageProcessingException when reading metadata." +
                    " Returning default orientation for image.", e);
        } catch (IOException e) {
            LOGGER.error("drewnoakes metadata extractor threw IOException when reading metadata." +
                    " Returning default orientation for image.", e);
        }

        return getNoFlipNoRotationImageTransform();
    }

    public static ImageTransform getImageTransform(File localFile) {
        try {
            //Attempt to retrieve the metadata from the image.
            Metadata metadata = ImageMetadataReader.readMetadata(localFile);
            return getImageTransformFromMetadata(metadata);
        } catch (ImageProcessingException e) {
            LOGGER.error("drewnoakes metadata extractor threw ImageProcessingException when reading metadata." +
                    " Returning default orientation for image.", e);
        } catch (IOException e) {
            LOGGER.error("drewnoakes metadata extractor threw IOException when reading metadata." +
                    " Returning default orientation for image.", e);
        }

        return getNoFlipNoRotationImageTransform();
    }

    private static ImageTransform getImageTransformFromMetadata(Metadata metadata) {
        //new ImageTransform(false, 0) is the original image orientation, with no flip needed, and no rotation needed.
        ImageTransform imageTransform = getNoFlipNoRotationImageTransform();

        if (metadata != null) {
            ExifIFD0Directory exifDir = metadata.getDirectory(ExifIFD0Directory.class);
            if (exifDir != null) {
                Integer orientationInteger = exifDir.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
                if (orientationInteger != null) {
                    imageTransform = convertOrientationToTransform(orientationInteger);
                }
            }
        }

        return imageTransform;
    }

    private static ImageTransform getNoFlipNoRotationImageTransform() {
        return new ImageTransform(false, 0);
    }

    /**
     * Converts an orientation number to an ImageTransform object used by Visallo.
     *
     * @param orientationInt The EXIF orientation number, from 1 - 8, representing the combinations of 4 different
     *                       rotations and 2 different flipped values.
     */
    public static ImageTransform convertOrientationToTransform(int orientationInt) {
        switch (orientationInt) {
            case 1:
                return getNoFlipNoRotationImageTransform();
            case 2:
                return new ImageTransform(true, 0);
            case 3:
                return new ImageTransform(false, 180);
            case 4:
                return new ImageTransform(true, 180);
            case 5:
                return new ImageTransform(true, 270);
            case 6:
                return new ImageTransform(false, 90);
            case 7:
                return new ImageTransform(true, 90);
            case 8:
                return new ImageTransform(false, 270);
            default:
                return getNoFlipNoRotationImageTransform();
        }
    }
}
