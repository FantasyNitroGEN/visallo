package org.visallo.core.model.properties.types;

import org.visallo.web.clientapi.model.VisibilityJson;

public class VisibilityJsonMetadataVisalloProperty extends ClientApiMetadataVisalloProperty<VisibilityJson> {
    public VisibilityJsonMetadataVisalloProperty(String key) {
        super(key, VisibilityJson.class);
    }
}
