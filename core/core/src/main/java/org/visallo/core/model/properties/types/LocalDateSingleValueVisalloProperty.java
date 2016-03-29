package org.visallo.core.model.properties.types;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * A VisalloProperty that converts a java.time.LocalDate object, which represents only a date without time information,
 * to an appropriate value for storage in Vertexium.
 */
public class LocalDateSingleValueVisalloProperty extends SingleValueVisalloProperty<LocalDate, Date> {

    public static final ZoneOffset TIME_ZONE = ZoneOffset.UTC;

    public LocalDateSingleValueVisalloProperty(String propertyName) {
        super(propertyName);
    }

    @Override
    public Date wrap(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().toInstant(TIME_ZONE));
    }

    @Override
    public LocalDate unwrap(Object value) {
        return value == null ?
                null : LocalDate.from(Instant.ofEpochMilli(((Date) value).getTime()).atOffset(TIME_ZONE));
    }

    public static LocalDate now() {
        return LocalDate.now(TIME_ZONE);
    }
}
