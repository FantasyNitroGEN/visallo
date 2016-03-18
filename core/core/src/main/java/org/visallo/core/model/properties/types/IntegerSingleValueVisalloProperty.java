package org.visallo.core.model.properties.types;

import org.vertexium.Element;

import java.util.Map;

public class IntegerSingleValueVisalloProperty extends IdentitySingleValueVisalloProperty<Integer> {
    public IntegerSingleValueVisalloProperty(String key) {
        super(key);
    }

    public Integer getPropertyValue(Element element, Integer defaultValue) {
        Integer nullable = getPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }

    public Integer getPropertyValue(Map<String, Object> map, Integer defaultValue) {
        Integer nullable = getPropertyValue(map);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
