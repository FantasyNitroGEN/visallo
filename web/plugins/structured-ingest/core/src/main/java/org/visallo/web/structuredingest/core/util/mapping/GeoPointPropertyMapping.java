package org.visallo.web.structuredingest.core.util.mapping;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.vertexium.type.GeoPoint;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.security.VisibilityTranslator;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoPointPropertyMapping extends PropertyMapping {
    public static final String PROPERTY_MAPPING_FORMAT_KEY = "format";
    public static final String PROPERTY_MAPPING_COLUMN_LAT_KEY = "columnLatitude";
    public static final String PROPERTY_MAPPING_COLUMN_LON_KEY = "columnLongitude";

    private static final String DECIMAL_NUMBER_REGEX = "(-?\\d*\\.?\\d+)";
    private static final String INTEGER_NUMBER_REGEX = "(-?\\d+)";
    private static final String DEGREES_REGEX = DECIMAL_NUMBER_REGEX + "\\D*?";
    private static final String DEGREES_MINUTES_REGEX = INTEGER_NUMBER_REGEX + "\\D+" + DECIMAL_NUMBER_REGEX + "\\D*?";
    private static final String DEGREES_MINUTES_SECONDS_REGEX = INTEGER_NUMBER_REGEX + "\\D+" + INTEGER_NUMBER_REGEX + "\\D+" + DECIMAL_NUMBER_REGEX + "\\D*?";

    public String latColumn;
    public String lonColumn;

    public enum Format {
        DECIMAL(DEGREES_REGEX + "[^\\d\\-]+" + DEGREES_REGEX),
        DEGREES_DECIMAL_MINUTES(DEGREES_MINUTES_REGEX + "[^\\d\\-]+" + DEGREES_MINUTES_REGEX),
        DEGREES_MINUTES_SECONDS(DEGREES_MINUTES_SECONDS_REGEX + "[^\\d\\-]+" + DEGREES_MINUTES_SECONDS_REGEX);

        private Pattern formatPattern;

        Format(String regex) {
            formatPattern = Pattern.compile(regex);
        }

        public double[] parse(String input) {
            Matcher matcher = formatPattern.matcher(input);
            if(matcher.matches()) {
                int totalGroupCount = matcher.groupCount();
                int componentGroupCount = totalGroupCount/2;

                return new double[] {
                        parseDouble(0, componentGroupCount, matcher),
                        parseDouble(1, componentGroupCount, matcher)
                };
            }
            throw new VisalloException("Unrecognized geo point format: " + input);
        }

        private double parseDouble(int index, int componentGroupCount, Matcher matcher) {
            int offset = index * componentGroupCount + 1;

            double result = Double.parseDouble(matcher.group(offset));
            if(componentGroupCount > 1) {
                result += (result < 0 ? -1 : 1) * Double.parseDouble(matcher.group(offset + 1)) / 60;
            }
            if(componentGroupCount > 2) {
                result += (result < 0 ? -1 : 1) * Double.parseDouble(matcher.group(offset + 2)) / 3600;
            }
            return result;
        }
    }

    public Format format;

    public GeoPointPropertyMapping(
            VisibilityTranslator visibilityTranslator, String workspaceId, JSONObject propertyMapping) {
        super(visibilityTranslator, workspaceId, propertyMapping);

        format = Format.valueOf(propertyMapping.getString(PROPERTY_MAPPING_FORMAT_KEY));

        latColumn = propertyMapping.optString(PROPERTY_MAPPING_COLUMN_LAT_KEY);
        lonColumn = propertyMapping.optString(PROPERTY_MAPPING_COLUMN_LON_KEY);

        if(Arrays.asList(value, key, latColumn, lonColumn).stream().allMatch(StringUtils::isBlank)){
            throw new VisalloException("You must provide one of: value, column, or latColumn/lonColumn");
        }
    }

    @Override
    public String extractRawValue(Map<String, Object> row) {
        if(StringUtils.isNotBlank(key) || !StringUtils.isBlank(value)) {
            return super.extractRawValue(row).toString();
        }

        String latRawValue = (String) row.get(latColumn);
        String lonRawValue = (String) row.get(lonColumn);

        if(StringUtils.isBlank(latRawValue) || StringUtils.isBlank(lonRawValue)) {
            return null;
        }

        return latRawValue.trim() + ", " + lonRawValue.trim();
    }

    @Override
    public Object decodeValue(Object rawPropertyValue) {
        if(rawPropertyValue instanceof String && !StringUtils.isBlank((String) rawPropertyValue)) {
            double[] latLon = format.parse((String) rawPropertyValue);
            return new GeoPoint(latLon[0], latLon[1]);
        }
        return null;
    }
}
