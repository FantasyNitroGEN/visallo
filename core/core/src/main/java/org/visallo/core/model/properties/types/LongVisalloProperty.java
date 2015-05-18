package org.visallo.core.model.properties.types;

import org.vertexium.Element;

public class LongVisalloProperty extends IdentityVisalloProperty<Long> {
    public LongVisalloProperty(String key) {
        super(key);
    }

    public long getPropertyValue(Element element, String propertyKey, long defaultValue) {
        Long nullable = getPropertyValue(element, propertyKey);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
