package org.visallo.core.model.properties.types;

import com.google.common.base.Function;
import org.vertexium.Property;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A VisalloProperty provides convenience methods for converting standard
 * property values to and from their raw types to the types required to
 * store them in a Vertexium instance.
 *
 * @param <TRaw>   the raw value type for this property
 * @param <TGraph> the value type presented to Vertexium for this property
 */
public abstract class VisalloPropertyBase<TRaw, TGraph> {
    private final String propertyName;
    private final Function<Object, TRaw> rawConverter;

    protected VisalloPropertyBase(final String propertyName) {
        this.propertyName = propertyName;
        this.rawConverter = new RawConverter();
    }

    /**
     * Convert the raw value to an appropriate value for storage
     * in Vertexium.
     */
    public abstract TGraph wrap(final TRaw value);

    /**
     * Convert the Vertexium value to its original raw type.
     */
    public abstract TRaw unwrap(final Object value);

    public final String getPropertyName() {
        return propertyName;
    }

    public boolean isSameName(Property property) {
        return isSameName(property.getName());
    }

    public boolean isSameName(String propertyName) {
        return this.propertyName.equals(propertyName);
    }

    protected Function<Object, TRaw> getRawConverter() {
        return rawConverter;
    }

    public TRaw getPropertyValue(Property property) {
        return unwrap(property.getValue());
    }

    protected class RawConverter implements Function<Object, TRaw> {
        @Override
        @SuppressWarnings("unchecked")
        public TRaw apply(final Object input) {
            return unwrap(input);
        }
    }

    protected boolean isEquals(TRaw newValue, TRaw currentValue) {
        checkNotNull(newValue, "newValue cannot be null");
        checkNotNull(currentValue, "currentValue cannot be null");
        return newValue.equals(currentValue);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "{propertyName='" + propertyName + "'}";
    }
}
