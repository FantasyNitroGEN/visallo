package org.visallo.web.structuredingest.mapping;

import org.json.JSONObject;
import org.junit.Test;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.structuredingest.core.util.mapping.DatePropertyMapping;
import org.visallo.web.structuredingest.core.util.mapping.PropertyMapping;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static junit.framework.TestCase.*;

public class DatePropertyMappingTest {

    @Test
    public void testDateValue() throws Exception {
        DatePropertyMapping propertyMapping = new DatePropertyMapping(null, null, buildJsonPropertyMapping());

        Date result = (Date)propertyMapping.decodeValue("2015-03-13 12:05:22");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("EST"));
        assertEquals("2015-03-13 17:05:22", sdf.format(result));
    }

    @Test
    public void testMalformedDateValue() throws Exception {
        DatePropertyMapping propertyMapping = new DatePropertyMapping(null, null, buildJsonPropertyMapping());

        try {
            propertyMapping.decodeValue("boston");
            fail("Should have thrown an error for the malformed date");
        } catch (VisalloException ve) {
            assertEquals("Unrecognized date value: boston", ve.getMessage());
        }
    }

    @Test
    public void testBlankValue() throws Exception {
        DatePropertyMapping propertyMapping = new DatePropertyMapping(null, null, buildJsonPropertyMapping());

        assertNull(propertyMapping.decodeValue("    "));
    }

    private JSONObject buildJsonPropertyMapping() {
        JSONObject jsonProperyMapping = new JSONObject();
        jsonProperyMapping.put(PropertyMapping.PROPERTY_MAPPING_NAME_KEY, "JUNIT");
        jsonProperyMapping.put(PropertyMapping.PROPERTY_MAPPING_KEY_KEY, 0);
        jsonProperyMapping.put(DatePropertyMapping.PROPERTY_MAPPING_DATE_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss");
        jsonProperyMapping.put(DatePropertyMapping.PROPERTY_MAPPING_DATE_TIMEZONE_KEY, "US/Hawaii");
        return jsonProperyMapping;
    }
}
