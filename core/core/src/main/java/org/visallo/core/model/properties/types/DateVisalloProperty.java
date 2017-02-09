package org.visallo.core.model.properties.types;

import org.vertexium.Element;
import org.visallo.core.model.graph.ElementUpdateContext;

import java.util.Date;

/**
 * A VisalloProperty that converts Dates to an appropriate value for
 * storage in Vertexium.
 */
public class DateVisalloProperty extends IdentityVisalloProperty<Date> {
    public DateVisalloProperty(String key) {
        super(key);
    }

    public <T extends Element> void updatePropertyIfValueIsNewer(
            ElementUpdateContext<T> ctx,
            String propertyKey,
            Date newValue,
            PropertyMetadata metadata,
            Long timestamp
    ) {
        if (isDateGreater(ctx.getElement(), propertyKey, newValue)) {
            updateProperty(ctx, propertyKey, newValue, metadata, timestamp);
        }
    }

    public <T extends Element> void updatePropertyIfValueIsNewer(
            ElementUpdateContext<T> ctx,
            String propertyKey,
            Date newValue,
            PropertyMetadata metadata
    ) {
        if (isDateGreater(ctx.getElement(), propertyKey, newValue)) {
            updateProperty(ctx, propertyKey, newValue, metadata);
        }
    }

    private <T extends Element> boolean isDateGreater(T element, String propertyKey, Date newValue) {
        if (element == null) {
            return true;
        }
        Date existingValue = getPropertyValue(element, propertyKey);
        if (existingValue == null) {
            return true;
        }
        return existingValue.compareTo(newValue) < 0;
    }
}
