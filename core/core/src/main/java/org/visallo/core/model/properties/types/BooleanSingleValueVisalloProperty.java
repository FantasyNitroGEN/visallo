package org.visallo.core.model.properties.types;

import org.vertexium.Element;

public class BooleanSingleValueVisalloProperty extends IdentitySingleValueVisalloProperty<Boolean> {
    public BooleanSingleValueVisalloProperty(String key) {
        super(key);
    }

    public boolean getPropertyValue(Element element, boolean defaultValue) {
        Boolean nullable = getPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
