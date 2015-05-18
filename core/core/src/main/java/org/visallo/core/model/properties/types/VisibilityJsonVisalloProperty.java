package org.visallo.core.model.properties.types;

import org.visallo.web.clientapi.model.VisibilityJson;
import org.vertexium.Element;

public class VisibilityJsonVisalloProperty extends ClientApiSingleValueVisalloProperty<VisibilityJson> {
    public VisibilityJsonVisalloProperty(String key) {
        super(key, VisibilityJson.class);
    }

    public VisibilityJson getPropertyValue(Element element, VisibilityJson defaultValue) {
        VisibilityJson value = getPropertyValue(element);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
