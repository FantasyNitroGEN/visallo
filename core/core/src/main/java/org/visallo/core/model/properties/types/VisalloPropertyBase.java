package org.visallo.core.model.properties.types;

import com.google.common.base.Function;

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

    protected Function<Object, TRaw> getRawConverter() {
        return rawConverter;
    }

    protected class RawConverter implements Function<Object, TRaw> {
        @Override
        @SuppressWarnings("unchecked")
        public TRaw apply(final Object input) {
            return unwrap(input);
        }
    }
}
