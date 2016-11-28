package org.visallo.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloJsonParseException;
import org.visallo.web.clientapi.util.ObjectMapperFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class JSONUtil {
    private static ObjectMapper mapper = ObjectMapperFactory.getInstance();

    public static JSONArray getOrCreateJSONArray(JSONObject json, String name) {
        JSONArray arr = json.optJSONArray(name);
        if (arr == null) {
            arr = new JSONArray();
            json.put(name, arr);
        }
        return arr;
    }

    public static boolean areEqual(Object o1, Object o2) throws JSONException {
        return fromJson(o1).equals(fromJson(o2));
    }

    public static void addToJSONArrayIfDoesNotExist(JSONArray jsonArray, Object value) {
        if (!arrayContains(jsonArray, value)) {
            jsonArray.put(value);
        }
    }

    public static boolean isInArray(JSONArray jsonArray, Object value) {
        return arrayIndexOf(jsonArray, value) >= 0;
    }

    public static int arrayIndexOf(JSONArray jsonArray, Object value) {
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.get(i).equals(value)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean arrayContains(JSONArray jsonArray, Object value) {
        return arrayIndexOf(jsonArray, value) != -1;
    }

    public static void removeFromJSONArray(JSONArray jsonArray, Object value) {
        int idx = arrayIndexOf(jsonArray, value);
        if (idx >= 0) {
            jsonArray.remove(idx);
        }
    }

    public static JSONObject parse(String jsonString) {
        try {
            return new JSONObject(jsonString);
        } catch (JSONException ex) {
            throw new VisalloJsonParseException(jsonString, ex);
        }
    }

    public static JSONArray parseArray(String s) {
        try {
            return new JSONArray(s);
        } catch (JSONException ex) {
            throw new VisalloJsonParseException(s, ex);
        }
    }

    public static JsonNode toJsonNode(JSONObject json) {
        try {
            if (json == null) {
                return null;
            }
            return mapper.readTree(json.toString());
        } catch (IOException e) {
            throw new VisalloException("Could not create json node from: " + json.toString(), e);
        }
    }

    public static Map<String, String> toStringMap(JSONObject json) {
        Map<String, String> results = new HashMap<String, String>();
        for (Object key : json.keySet()) {
            String keyStr = (String) key;
            results.put(keyStr, json.getString(keyStr));
        }
        return results;
    }

    public static List<String> toStringList(JSONArray arr) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < arr.length(); i++) {
            result.add(arr.getString(i));
        }
        return result;
    }

    public static List<Object> toList(JSONArray arr) {
        List<Object> list = new ArrayList();
        for (int i = 0; i < arr.length(); i++) {
            list.add(fromJson(arr.get(i)));
        }
        return list;
    }

    public static Map<String, Object> toMap(JSONObject obj) {
        Iterator<String> keys = obj.keys();
        Map<String, Object> map = new HashMap<>();
        while (keys.hasNext()) {
            String key = keys.next();
            map.put(key, fromJson(obj.get(key)));
        }
        return map;
    }

    public static JSONObject toJson(Map<String, ?> map) {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, ?> e : map.entrySet()) {
            json.put(e.getKey(), toJson(e.getValue()));
        }
        return json;
    }

    public static Object toJson(Object value) {
        if (value instanceof Map) {
            return toJson((Map) value);
        } else if (value instanceof Iterable) {
            return toJson((Iterable) value);
        } else {
            return value;
        }
    }

    public static JSONArray toJson(Iterable iterable) {
        JSONArray json = new JSONArray();
        for (Object o : iterable) {
            json.put(toJson(o));
        }
        return json;
    }

    public static Long getOptionalLong(JSONObject json, String fieldName) {
        if (!json.has(fieldName) || json.isNull(fieldName)) {
            return null;
        }
        return json.getLong(fieldName);
    }

    private static Object fromJson(Object elem) throws JSONException {
        if (elem instanceof JSONObject) {
            return toMap((JSONObject) elem);
        } else if (elem instanceof JSONArray) {
            return toList((JSONArray) elem);
        } else {
            return elem;
        }
    }

    public static Stream<Object> stream(JSONArray jsonArray) {
        return toList(jsonArray).stream();
    }
}
