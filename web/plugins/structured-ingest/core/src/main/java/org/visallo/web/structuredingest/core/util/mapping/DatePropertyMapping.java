package org.visallo.web.structuredingest.core.util.mapping;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.security.VisibilityTranslator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class DatePropertyMapping extends PropertyMapping {
    public static final String PROPERTY_MAPPING_DATE_FORMAT_KEY = "format";
    public static final String PROPERTY_MAPPING_DATE_TIMEZONE_KEY = "timezone";

    public SimpleDateFormat dateFormat;

    public DatePropertyMapping(VisibilityTranslator visibilityTranslator, String workspaceId, JSONObject propertyMapping) {
        super(visibilityTranslator, workspaceId, propertyMapping);

        //if (propertyMapping.getBoolean("requiresTransform"))
        if (propertyMapping.has(PROPERTY_MAPPING_DATE_FORMAT_KEY)) {
            String format = propertyMapping.getString(PROPERTY_MAPPING_DATE_FORMAT_KEY);
            String timezone = propertyMapping.getString(PROPERTY_MAPPING_DATE_TIMEZONE_KEY);
            if (StringUtils.isBlank(format) || StringUtils.isBlank(timezone)) {
                throw new VisalloException("Both format and timezone are required for the Date propery " + name);
            }

            dateFormat = new SimpleDateFormat(format);
            dateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
        }
    }

    @Override
    public Object decodeValue(Object rawPropertyValue) {
        if (rawPropertyValue instanceof String) {
            String strPropertyValue = (String) rawPropertyValue;
            if (StringUtils.isBlank(strPropertyValue)) {
                return null;
            } else {
                try {
                    return dateFormat.parse(strPropertyValue);
                } catch (ParseException pe) {
                    throw new VisalloException("Unrecognized date value: " + rawPropertyValue, pe);
                }
            }
        }

        return rawPropertyValue;
    }
}
