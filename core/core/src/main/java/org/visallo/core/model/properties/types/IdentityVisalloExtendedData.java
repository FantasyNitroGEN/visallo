package org.visallo.core.model.properties.types;

public abstract class IdentityVisalloExtendedData<T> extends VisalloExtendedData<T, T> {
    public IdentityVisalloExtendedData(String tableName, String propertyName) {
        super(tableName, propertyName);
    }

    @Override
    public T rawToGraph(T value) {
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T graphToRaw(final Object value) {
        return (T) value;
    }
}
