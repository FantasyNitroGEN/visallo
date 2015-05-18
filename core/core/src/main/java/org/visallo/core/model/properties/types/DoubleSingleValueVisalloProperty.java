package org.visallo.core.model.properties.types;

import org.vertexium.Element;

public class DoubleSingleValueVisalloProperty extends IdentitySingleValueVisalloProperty<Double> {
    public DoubleSingleValueVisalloProperty(String key) {
        super(key);
    }

    public double getPropertyValue(Element element, double defaultValue) {
        Double nullable = getPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
