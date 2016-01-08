package org.visallo.core.model.properties.types;

public class VisalloPropertyUpdateRemove extends VisalloPropertyUpdate {
    private final long beforeDeleteTimestamp;
    private final boolean isDeleted;
    private final boolean isHidden;

    public VisalloPropertyUpdateRemove(VisalloProperty property, String propertyKey, long beforeDeleteTimestamp, boolean isDeleted, boolean isHidden) {
        super(property, propertyKey);
        this.beforeDeleteTimestamp = beforeDeleteTimestamp;
        this.isDeleted = isDeleted;
        this.isHidden = isHidden;
    }

    public VisalloPropertyUpdateRemove(SingleValueVisalloProperty property, long beforeDeleteTimestamp, boolean isDeleted, boolean isHidden) {
        super(property);
        this.beforeDeleteTimestamp = beforeDeleteTimestamp;
        this.isDeleted = isDeleted;
        this.isHidden = isHidden;
    }

    public long getBeforeDeleteTimestamp() {
        return beforeDeleteTimestamp;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public boolean isHidden() {
        return isHidden;
    }
}
