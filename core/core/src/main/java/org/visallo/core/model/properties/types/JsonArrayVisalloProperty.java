package org.visallo.core.model.properties.types;

import org.json.JSONArray;
import org.visallo.core.util.JSONUtil;

public class JsonArrayVisalloProperty extends VisalloProperty<JSONArray, String> {
    public JsonArrayVisalloProperty(String key) {
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

    @Override
    protected boolean isEquals(JSONArray newValue, JSONArray currentValue) {
        return JSONUtil.areEqual(newValue, currentValue);
    }
}
