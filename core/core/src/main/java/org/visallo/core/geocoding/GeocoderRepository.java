package org.visallo.core.geocoding;

import org.visallo.core.model.workQueue.Priority;
import org.vertexium.ElementType;
import org.vertexium.Visibility;

import java.util.List;

public abstract class GeocoderRepository {
    public abstract List<GeocodeResult> find(String query);

    public abstract void queuePropertySet(
            String locationString,
            ElementType elementType,
            String elementId,
            String propertyKey,
            String propertyName,
            Visibility visibility,
            Priority priority
    );
}
