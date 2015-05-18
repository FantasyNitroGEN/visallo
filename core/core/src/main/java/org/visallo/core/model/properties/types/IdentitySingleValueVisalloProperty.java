package org.visallo.core.model.properties.types;

public class IdentitySingleValueVisalloProperty<T> extends SingleValueVisalloProperty<T, T> {
    public IdentitySingleValueVisalloProperty(final String propertyName) {
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
