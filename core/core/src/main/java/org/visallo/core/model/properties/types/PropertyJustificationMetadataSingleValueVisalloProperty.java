package org.visallo.core.model.properties.types;

import org.visallo.core.model.PropertyJustificationMetadata;
import org.json.JSONObject;

public class PropertyJustificationMetadataSingleValueVisalloProperty extends SingleValueVisalloProperty<PropertyJustificationMetadata, String> {
    public PropertyJustificationMetadataSingleValueVisalloProperty(final String key) {
        super(key);
    }

    @Override
    public String wrap(PropertyJustificationMetadata value) {
        return value.toJson().toString();
    }

    @Override
    public PropertyJustificationMetadata unwrap(Object value) {
        if (value == null) {
            return null;
        }
        return new PropertyJustificationMetadata(new JSONObject(value.toString()));
    }
}
