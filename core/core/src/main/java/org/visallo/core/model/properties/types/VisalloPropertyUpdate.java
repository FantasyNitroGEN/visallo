package org.visallo.core.model.properties.types;

import org.vertexium.mutation.ElementMutation;

public class VisalloPropertyUpdate {
    private final VisalloPropertyBase property;
    private final String propertyKey;

    public VisalloPropertyUpdate(VisalloProperty property, String propertyKey) {
        this.property = property;
        this.propertyKey = propertyKey;
    }

    public VisalloPropertyUpdate(SingleValueVisalloProperty property) {
        this.property = property;
        this.propertyKey = ElementMutation.DEFAULT_KEY;
    }

    public VisalloPropertyBase getProperty() {
        return property;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public String getPropertyName() {
        return getProperty().getPropertyName();
    }
}
