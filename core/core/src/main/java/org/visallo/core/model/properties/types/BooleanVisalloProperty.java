package org.visallo.core.model.properties.types;

import org.vertexium.Element;

public class BooleanVisalloProperty extends IdentityVisalloProperty<Boolean> {
    public BooleanVisalloProperty(String key) {
        super(key);
    }

    public boolean getPropertyValue(Element element, String propertyKey, boolean defaultValue) {
        Boolean nullable = getPropertyValue(element, propertyKey);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }

    public boolean getFirstPropertyValue(Element element, boolean defaultValue) {
        Boolean nullable = getOnlyPropertyValue(element);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
