package org.visallo.imageMetadataHelper;

import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.GpsDirectory;
import org.vertexium.type.GeoPoint;

public class GeoPointExtractor {

    public static GeoPoint getGeoPoint(Metadata metadata) {
        GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gpsDir != null) {
            GeoLocation geoLocation = gpsDir.getGeoLocation();
            if (geoLocation != null) {
                Double latitude = geoLocation.getLatitude();
                Double longitude = geoLocation.getLongitude();
                Double altitude = null;
                try {
                    altitude = gpsDir.getDouble(GpsDirectory.TAG_ALTITUDE);
                } catch (MetadataException e) {
                    //No code needed. Altitude is already null.
                }

                if (latitude != null && latitude != 0 && longitude != null && longitude != 0) {
                    if (altitude != null && altitude != 0) {
                        GeoPoint geoPoint = new GeoPoint(latitude, longitude, altitude);
                        return geoPoint;
                    } else {
                        GeoPoint geoPoint = new GeoPoint(latitude, longitude);
                        return geoPoint;
                    }
                }
            }

        }
        return null;
    }

}
