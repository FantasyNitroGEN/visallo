package org.visallo.core.model.properties.types;

public class IdentityMetadataVisalloProperty<T> extends MetadataVisalloProperty<T, T> {
    /**
     * Create a new IdentityVisalloProperty.
     *
     * @param propertyName the property name
     */
    public IdentityMetadataVisalloProperty(final String propertyName) {
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
