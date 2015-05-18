package org.visallo.core.model.properties.types;

import org.vertexium.Element;

public class IntegerSingleValueVisalloProperty extends IdentitySingleValueVisalloProperty<Integer> {
    public IntegerSingleValueVisalloProperty(String key) {
        super(key);
    }

    public int getPropertyValue(Element element, int defaultValue) {
        Integer nullable = getPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
