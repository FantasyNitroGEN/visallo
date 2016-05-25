package org.visallo.core.model.properties.types;

import java.time.LocalDate;
import java.util.Date;

/**
 * A multi-value property that converts a java.time.LocalDate object, which represents only a date without time
 * information, to an appropriate value for storage in Vertexium.
 */
public class LocalDateVisalloProperty
        extends VisalloProperty<LocalDate, Date>
        implements WrapsLocalDate {

    public LocalDateVisalloProperty(String propertyName) {
        super(propertyName);
    }

    @Override
    public Date wrap(LocalDate localDate) {
        return WrapsLocalDate.super.wrap(localDate);
    }

    @Override
    public LocalDate unwrap(Object value) {
        return WrapsLocalDate.super.unwrap(value);
    }

    public static LocalDate now() {
        return WrapsLocalDate.now();
    }
}
