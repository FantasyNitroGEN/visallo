package org.visallo.core.model.properties.types;

/**
 * A VisalloProperty whose raw and Vertexium types are the same.
 */
public class IdentityVisalloProperty<T> extends VisalloProperty<T, T> {
    /**
     * Create a new IdentityVisalloProperty.
     * @param propertyName the property name
     */
    public IdentityVisalloProperty(final String propertyName) {
        super(propertyName);
    }

    @Override
    public T wrap(final T value) {
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T unwrap(final Object value) {
        return (T) value;
    }
}
