package org.visallo.imageMetadataHelper;

import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class DimensionsExtractor {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(DimensionsExtractor.class);

    private static enum Dimension {
        WIDTH, HEIGHT
    }

    /**
     * Checks the metadata directories in order until the width is found. The first match found in a directory
     * is returned.
     *
     * @param metadata
     * @return
     */
    public static Integer getWidthViaMetadata(Metadata metadata) {
        return getDimensionViaMetadata(metadata, Dimension.WIDTH);
    }

    /**
     * Checks the metadata directories in order until the height is found. The first match found in a directory
     * is returned.
     *
     * @param metadata
     * @return
     */
    public static Integer getHeightViaMetadata(Metadata metadata) {
        return getDimensionViaMetadata(metadata, Dimension.HEIGHT);
    }


    private static Integer getDimensionViaMetadata(Metadata metadata, DimensionsExtractor.Dimension dimensionType) {
        if (dimensionType == null ){
            return null;
        }

        String tagName = dimensionType == Dimension.WIDTH ? "Image Width" : "Image Height";
        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                if(tagName.equalsIgnoreCase(tag.getTagName())) {
                    Integer dimension = directory.getInteger(tag.getTagType());
                    if(dimension != null) {
                        return dimension;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Get the width of the image file by loading the file as a buffered image.
     * @return
     */
    public static Integer getWidthViaBufferedImage(File imageFile){
        try {
            BufferedImage bufImage = ImageIO.read(imageFile);
            int width = bufImage.getWidth();
            return width;
        } catch (IOException e){
            if (imageFile != null) {
                LOGGER.debug("Could not read imageFile: " + imageFile.getName());
            }
        }
        return null;
    }

    /**
     * Get the height of the image file by loading the file as a buffered image.
     * @return
     */
    public static Integer getHeightViaBufferedImage(File imageFile){
        try {
            BufferedImage bufImage = ImageIO.read(imageFile);
            int height = bufImage.getHeight();
            return height;
        } catch (IOException e){
            if (imageFile != null) {
                LOGGER.debug("Could not read imageFile: " + imageFile.getName());
            }
        }
        return null;
    }

}
