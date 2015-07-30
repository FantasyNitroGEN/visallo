package org.visallo.imageMetadataHelper;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.xmp.XmpDirectory;
import org.apache.commons.lang.StringUtils;

public class MakeExtractor {

    /**
     * Checks the metadata directories in order until the make is found. The first match found in a directory
     * is returned.
     *
     * @param metadata
     * @return
     */
    public static String getMake(Metadata metadata) {

        String makeString = null;

        ExifIFD0Directory exifDir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (exifDir != null) {
            makeString = exifDir.getDescription(ExifIFD0Directory.TAG_MAKE);
            if (!StringUtils.isBlank(makeString) && !"none".equals(makeString)) {
                return makeString;
            }
        }

        XmpDirectory xmpDir = metadata.getFirstDirectoryOfType(XmpDirectory.class);
        if (xmpDir != null) {
            makeString = xmpDir.getDescription(XmpDirectory.TAG_MAKE);
            if (!StringUtils.isBlank(makeString) && !"none".equals(makeString)) {
                return makeString;
            }
        }

        return null;
    }
}
