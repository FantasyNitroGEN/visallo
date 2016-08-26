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

    @Override
    public String toString() {
        return "VisalloPropertyUpdate{" +
                "property=" + property.getPropertyName() +
                ", propertyKey='" + propertyKey + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VisalloPropertyUpdate that = (VisalloPropertyUpdate) o;

        if (!property.equals(that.property)) {
            return false;
        }
        if (!propertyKey.equals(that.propertyKey)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = property.hashCode();
        result = 31 * result + propertyKey.hashCode();
        return result;
    }
}
