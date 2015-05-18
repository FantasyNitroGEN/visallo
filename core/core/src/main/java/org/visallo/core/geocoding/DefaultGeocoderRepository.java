package org.visallo.core.geocoding;

import org.visallo.core.model.workQueue.Priority;
import org.vertexium.ElementType;
import org.vertexium.Visibility;

import java.util.ArrayList;
import java.util.List;

public class DefaultGeocoderRepository extends GeocoderRepository {
    @Override
    public List<GeocodeResult> find(String query) {
        return new ArrayList<>();
    }

    @Override
    public void queuePropertySet(
            String locationString,
            ElementType elementType,
            String elementId,
            String propertyKey,
            String propertyName,
            Visibility visibility,
            Priority priority
    ) {
    }
}
