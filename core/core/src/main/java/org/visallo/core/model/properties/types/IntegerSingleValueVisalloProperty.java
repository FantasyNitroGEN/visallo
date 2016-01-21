package org.visallo.core.model.properties.types;

import org.vertexium.Element;

import java.util.Map;

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

    public int getPropertyValue(Map<String, Object> map, int defaultValue) {
        Integer nullable = getPropertyValue(map);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
