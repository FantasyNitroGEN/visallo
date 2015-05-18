package org.visallo.core.model.properties.types;

import org.visallo.core.ingest.ArtifactDetectedObject;
import org.json.JSONObject;

public class DetectedObjectProperty extends VisalloProperty<ArtifactDetectedObject, String> {
    public DetectedObjectProperty(String key) {
        super(key);
    }

    @Override
    public String wrap(ArtifactDetectedObject value) {
        return value.toJson().toString();
    }

    @Override
    public ArtifactDetectedObject unwrap(Object value) {
        if (value == null) {
            return null;
        }
        return new ArtifactDetectedObject(new JSONObject(value.toString()));
    }
}
