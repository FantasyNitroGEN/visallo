package org.visallo.core.model.properties.types;

import org.vertexium.Element;

public class DoubleVisalloProperty extends IdentityVisalloProperty<Double> {
    public DoubleVisalloProperty(String key) {
        super(key);
    }

    public Double getPropertyValue(Element element, String propertyKey, Double defaultValue) {
        Double nullable = getPropertyValue(element, propertyKey);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }

    public Double getFirstPropertyValue(Element element, Double defaultValue) {
        Double nullable = getOnlyPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
