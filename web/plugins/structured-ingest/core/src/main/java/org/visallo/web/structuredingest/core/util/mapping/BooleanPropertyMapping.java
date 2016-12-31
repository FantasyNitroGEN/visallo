package org.visallo.web.structuredingest.core.util.mapping;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.security.VisibilityTranslator;

import java.util.ArrayList;
import java.util.List;

public class BooleanPropertyMapping extends PropertyMapping {
    public static final String PROPERTY_MAPPING_BOOLEAN_TRUE_KEY = "trueValues";
    public static final String PROPERTY_MAPPING_BOOLEAN_FALSE_KEY = "falseValues";
    public static final String PROPERTY_MAPPING_DEFAULT_KEY = "defaultValue";

    public List<String> trueValues = new ArrayList<>();
    public List<String> falseValues = new ArrayList<>();

    private Boolean defaultValue;

    public BooleanPropertyMapping(
            VisibilityTranslator visibilityTranslator, String workspaceId, JSONObject propertyMapping) {
        super(visibilityTranslator, workspaceId, propertyMapping);

        String defaultValueStr = propertyMapping.optString(PROPERTY_MAPPING_DEFAULT_KEY);
        if(!StringUtils.isBlank(defaultValueStr)) {
            defaultValue = Boolean.parseBoolean(defaultValueStr);
        }

        JSONArray trueValueMappings = propertyMapping.getJSONArray(PROPERTY_MAPPING_BOOLEAN_TRUE_KEY);
        JSONArray falseValueMappings = propertyMapping.getJSONArray(PROPERTY_MAPPING_BOOLEAN_FALSE_KEY);

        for (int i = 0; i < trueValueMappings.length(); i++) {
            trueValues.add(trueValueMappings.getString(i).toLowerCase());
        }
        for (int i = 0; i < falseValueMappings.length(); i++) {
            falseValues.add(falseValueMappings.getString(i).toLowerCase());
        }
    }

    @Override
    public Object decodeValue(Object rawPropertyValue) {
        Boolean result = defaultValue;
        if (rawPropertyValue instanceof String && !StringUtils.isBlank((String)rawPropertyValue)) {
            String value = (String) rawPropertyValue;
            rawPropertyValue = value.toLowerCase();
            if (trueValues.contains(rawPropertyValue)) {
                result = Boolean.TRUE;
            } else if (falseValues.contains(rawPropertyValue)) {
                result = Boolean.FALSE;
            } else {
                throw new VisalloException("Unrecognized boolean value: " + rawPropertyValue);
            }
        }

        return result;
    }
}
