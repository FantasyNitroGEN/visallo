package org.visallo.core.model.properties.types;

import java.util.Date;

/**
 * A VisalloProperty that converts Dates to an appropriate value for
 * storage in Vertexium.
 */
public class DateVisalloProperty extends IdentityVisalloProperty<Date> {
    public DateVisalloProperty(String key) {
        super(key);
    }
}
