package org.visallo.core.model.properties.types;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

public interface WrapsLocalDate {
    ZoneOffset TIME_ZONE = ZoneOffset.UTC;

    default Date wrap(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().toInstant(TIME_ZONE));
    }

    default LocalDate unwrap(Object value) {
        return value == null ?
                null : LocalDate.from(Instant.ofEpochMilli(((Date) value).getTime()).atOffset(TIME_ZONE));
    }

    static LocalDate now() {
        return LocalDate.now(TIME_ZONE);
    }
}
