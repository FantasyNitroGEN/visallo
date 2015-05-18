package org.visallo.core.cmdline.converters;

import com.beust.jcommander.IStringConverter;
import org.vertexium.type.GeoPoint;

public class GeoPointConverter implements IStringConverter<GeoPoint> {
    @Override
    public GeoPoint convert(String s) {
        return GeoPoint.parse(s);
    }
}
