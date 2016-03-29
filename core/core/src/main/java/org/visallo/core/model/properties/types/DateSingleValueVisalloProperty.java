package org.visallo.core.model.properties.types;

import java.util.Date;

/**
 * A VisalloProperty that converts a legacy java.util.Date object, which represents an instant in time, to an
 * appropriate value for storage in Vertexium.
 */
public class DateSingleValueVisalloProperty extends IdentitySingleValueVisalloProperty<Date> {
    public DateSingleValueVisalloProperty(String propertyName) {
        super(propertyName);
    }
}
