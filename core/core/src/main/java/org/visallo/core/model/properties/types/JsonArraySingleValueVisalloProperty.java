package org.visallo.core.model.properties.types;

import org.visallo.core.util.JSONUtil;
import org.json.JSONArray;

public class JsonArraySingleValueVisalloProperty extends SingleValueVisalloProperty<JSONArray, String> {
    public JsonArraySingleValueVisalloProperty(String key) {
        super(key);
    }

    @Override
    public String wrap(JSONArray value) {
        return value.toString();
    }

    @Override
    public JSONArray unwrap(Object value) {
        if (value == null) {
            return null;
        }
        return JSONUtil.parseArray(value.toString());
    }
}
