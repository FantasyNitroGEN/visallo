package org.visallo.web.clientapi.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.IOException;
import java.util.*;

public class ClientApiConverter {
    public static Object toClientApiValue(Object value) {
        if (value instanceof JSONArray) {
            return toClientApiValue((JSONArray) value);
        } else if (value instanceof JSONObject) {
            return toClientApiValueInternal((JSONObject) value);
        } else if (JSONObject.NULL.equals(value)) {
            return null;
        } else if (value instanceof String) {
            return toClientApiValue((String) value);
        } else if (value instanceof Date) {
            return toClientApiValue(((Date) value).getTime());
        }
        return value;
    }

    private static List<Object> toClientApiValue(JSONArray json) {
        List<Object> result = new ArrayList<Object>();
        for (int i = 0; i < json.length(); i++) {
            Object obj = json.get(i);
            result.add(toClientApiValue(obj));
        }
        return result;
    }

    private static Object toClientApiValue(String value) {
        try {
            String valueString = value;
            valueString = valueString.trim();
            if (valueString.startsWith("{") && valueString.endsWith("}")) {
                return toClientApiValue((Object) new JSONObject(valueString));
            }
        } catch (Exception ex) {
            // ignore this exception it just mean the string wasn't really json
        }
        return value;
    }

    private static Object toClientApiValueInternal(JSONObject json) {
        if (json.length() == 2 && json.has("source") && json.has("workspaces")) {
            VisibilityJson visibilityJson = new VisibilityJson();
            visibilityJson.setSource(json.getString("source"));
            JSONArray workspacesJson = json.getJSONArray("workspaces");
            for (int i = 0; i < workspacesJson.length(); i++) {
                visibilityJson.addWorkspace(workspacesJson.getString(i));
            }
            return visibilityJson;
        }
        return toClientApiValue(json);
    }

    public static Map<String, Object> toClientApiValue(JSONObject json) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (Object key : json.keySet()) {
            String keyStr = (String) key;
            result.put(keyStr, toClientApiValue(json.get(keyStr)));
        }
        return result;
    }

    public static Object fromClientApiValue(Object obj) {
        if (obj instanceof Map) {
            Map map = (Map) obj;
            if (map.size() == 2 && map.containsKey("source") && map.containsKey("workspaces")) {
                VisibilityJson visibilityJson = new VisibilityJson();
                visibilityJson.setSource((String) map.get("source"));
                List<String> workspaces = (List<String>) map.get("workspaces");
                for (String workspace : workspaces) {
                    visibilityJson.addWorkspace(workspace);
                }
                return visibilityJson;
            }
        }
        return obj;
    }

    public static String clientApiToString(Object o) {
        if (o == null) {
            throw new RuntimeException("o cannot be null.");
        }
        try {
            return ObjectMapperFactory.getInstance().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not convert object '" + o.getClass().getName() + "' to string", e);
        }
    }

    public static <T> T toClientApi(String str, Class<T> clazz) {
        try {
            return ObjectMapperFactory.getInstance().readValue(str, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Could not parse '" + str + "' to class '" + clazz.getName() + "'", e);
        }
    }
}
