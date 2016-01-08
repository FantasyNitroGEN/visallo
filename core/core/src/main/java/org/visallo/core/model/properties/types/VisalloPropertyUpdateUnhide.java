package org.visallo.core.model.properties.types;

public class VisalloPropertyUpdateUnhide extends VisalloPropertyUpdate {
    public VisalloPropertyUpdateUnhide(VisalloProperty property, String propertyKey) {
        super(property, propertyKey);
    }

    public VisalloPropertyUpdateUnhide(SingleValueVisalloProperty property) {
        super(property);
    }
}
