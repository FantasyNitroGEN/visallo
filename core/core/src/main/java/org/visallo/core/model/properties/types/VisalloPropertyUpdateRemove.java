package org.visallo.core.model.properties.types;

public class VisalloPropertyUpdateRemove extends VisalloPropertyUpdate {
    private final long beforeDeleteTimestamp;

    public VisalloPropertyUpdateRemove(VisalloProperty property, String propertyKey, long beforeDeleteTimestamp) {
        super(property, propertyKey);
        this.beforeDeleteTimestamp = beforeDeleteTimestamp;
    }

    public VisalloPropertyUpdateRemove(SingleValueVisalloProperty property, long beforeDeleteTimestamp) {
        super(property);
        this.beforeDeleteTimestamp = beforeDeleteTimestamp;
    }

    public long getBeforeDeleteTimestamp() {
        return beforeDeleteTimestamp;
    }
}
