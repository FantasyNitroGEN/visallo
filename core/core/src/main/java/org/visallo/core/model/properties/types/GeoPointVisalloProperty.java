package org.visallo.core.model.properties.types;

import org.vertexium.Element;
import org.vertexium.type.GeoPoint;

public class GeoPointVisalloProperty extends IdentityVisalloProperty<GeoPoint> {
    public GeoPointVisalloProperty(String propertyName) {
        super(propertyName);
    }

    public GeoPoint getPropertyValue(Element element, String propertyKey, GeoPoint defaultValue) {
        GeoPoint nullable = getPropertyValue(element, propertyKey);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
