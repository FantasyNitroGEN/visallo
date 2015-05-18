package org.visallo.core.model.properties.types;

import org.vertexium.Element;

public class LongSingleValueVisalloProperty extends IdentitySingleValueVisalloProperty<Long> {
    public LongSingleValueVisalloProperty(String key) {
        super(key);
    }

    public long getPropertyValue(Element element, long defaultValue) {
        Long nullable = getPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
