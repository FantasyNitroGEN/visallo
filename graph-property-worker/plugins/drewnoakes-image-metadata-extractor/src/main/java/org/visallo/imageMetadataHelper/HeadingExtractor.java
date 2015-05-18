package org.visallo.imageMetadataHelper;

import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.GpsDirectory;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public class HeadingExtractor {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(HeadingExtractor.class);

    public static Double getImageHeading(Metadata metadata) {
        GpsDirectory gpsDir = metadata.getDirectory(GpsDirectory.class);
        if (gpsDir != null) {
            //TODO. Assumes true direction for IMG_DIRECTION. Can check TAG_GPS_IMG_DIRECTION_REF to be more specific.
            try {
                Double imageHeading = gpsDir.getDouble(GpsDirectory.TAG_GPS_IMG_DIRECTION);
                return imageHeading;
            } catch (MetadataException e) {
                LOGGER.debug("getDouble(TAG_GPS_IMAGE_DIRECTION) threw MetadataException when attempting to" +
                        "retrieve GPS Heading.");
            }
        }
        return null;
    }

}
