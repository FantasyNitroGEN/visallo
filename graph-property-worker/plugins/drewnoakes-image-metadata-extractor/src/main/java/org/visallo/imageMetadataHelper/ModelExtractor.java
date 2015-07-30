package org.visallo.imageMetadataHelper;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.xmp.XmpDirectory;
import org.apache.commons.lang.StringUtils;

public class ModelExtractor {

    /**
     * Checks the metadata directories in order until the model is found. The first match found in a directory
     * is returned.
     *
     * @param metadata
     * @return
     */
    public static String getModel(Metadata metadata) {

        String modelString = null;

        ExifIFD0Directory exifDir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (exifDir != null) {
            modelString = exifDir.getDescription(ExifIFD0Directory.TAG_MODEL);
            if (!StringUtils.isBlank(modelString) && !"none".equals(modelString)) {
                return modelString;
            }
        }

        XmpDirectory xmpDir = metadata.getFirstDirectoryOfType(XmpDirectory.class);
        if (xmpDir != null) {
            modelString = xmpDir.getDescription(XmpDirectory.TAG_MODEL);
            if (!StringUtils.isBlank(modelString) && !"none".equals(modelString)) {
                return modelString;
            }
        }
        return null;
    }
}
