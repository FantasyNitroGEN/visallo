package org.visallo.core.model.properties.types;

import org.vertexium.Element;

public class IntegerVisalloProperty extends IdentityVisalloProperty<Integer> {
    public IntegerVisalloProperty(String key) {
        super(key);
    }

    public Integer getPropertyValue(Element element, String propertyKey, Integer defaultValue) {
        Integer nullable = getPropertyValue(element, propertyKey);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }

    public Integer getOnlyPropertyValue(Element element, Integer defaultValue) {
        Integer nullable = getOnlyPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
