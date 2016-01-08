package org.visallo.core.ingest.graphProperty;

import org.visallo.core.model.properties.types.VisalloPropertyUpdate;
import org.visallo.core.model.properties.types.VisalloPropertyUpdateRemove;
import org.visallo.core.model.properties.types.VisalloPropertyUpdateUnhide;

public enum ElementOrPropertyStatus {
    HIDDEN,
    UNHIDDEN,
    DELETION,
    UPDATE;

    public static ElementOrPropertyStatus safeParse(String status) {
        try {
            if (status == null || status.length() == 0) {
                return ElementOrPropertyStatus.UPDATE;
            }
            return ElementOrPropertyStatus.valueOf(status);
        } catch (Exception ex) {
            return ElementOrPropertyStatus.UPDATE;
        }
    }

    public static ElementOrPropertyStatus getStatus (VisalloPropertyUpdate propertyUpdate) {
        if (propertyUpdate instanceof VisalloPropertyUpdateRemove && ((VisalloPropertyUpdateRemove) propertyUpdate).isDeleted()) {
            return ElementOrPropertyStatus.DELETION;
        }
        if (propertyUpdate instanceof VisalloPropertyUpdateRemove && ((VisalloPropertyUpdateRemove) propertyUpdate).isHidden()) {
            return ElementOrPropertyStatus.HIDDEN;
        }

        if (propertyUpdate instanceof VisalloPropertyUpdateUnhide) {
            return ElementOrPropertyStatus.UNHIDDEN;
        }

        return ElementOrPropertyStatus.UPDATE;
    }
}
