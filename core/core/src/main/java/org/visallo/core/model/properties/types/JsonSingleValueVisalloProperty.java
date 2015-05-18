package org.visallo.core.model.properties.types;

import org.visallo.core.util.JSONUtil;
import org.json.JSONObject;

public class JsonSingleValueVisalloProperty extends SingleValueVisalloProperty<JSONObject, String> {
    public JsonSingleValueVisalloProperty(String key) {
        super(key);
    }

    @Override
    public String wrap(JSONObject value) {
        return value.toString();
    }

    @Override
    public JSONObject unwrap(Object value) {
        if (value == null) {
            return null;
        }
        return JSONUtil.parse(value.toString());
    }
}
