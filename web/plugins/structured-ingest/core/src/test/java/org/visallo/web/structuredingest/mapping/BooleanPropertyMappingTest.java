package org.visallo.web.structuredingest.mapping;

import com.google.common.collect.Maps;
import org.json.JSONObject;
import org.junit.Test;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.structuredingest.core.util.mapping.BooleanPropertyMapping;
import org.visallo.web.structuredingest.core.util.mapping.PropertyMapping;

import java.util.*;

import static junit.framework.TestCase.*;
import static org.visallo.web.structuredingest.mapping.MappingTestHelpers.createIndexedMap;

public class BooleanPropertyMappingTest {
    private List<String> trueValues = Arrays.asList(new String[] { "yes", "y", "ok", "1" });
    private List<String> falseValues = Arrays.asList(new String[] { "no", "omg no", "0" });

    @Test
    public void testTrueValues() throws Exception {
        BooleanPropertyMapping propertyMapping = new BooleanPropertyMapping(null, null, buildJsonPropertyMapping());
        Map<String, Object> map = Maps.newHashMap();
        for (String trueValue : trueValues) {
            map.put("" + 0, trueValue);
            assertTrue((Boolean) propertyMapping.decodeValue(map));
        }
    }

    @Test
    public void testFalseValues() throws Exception {
        BooleanPropertyMapping propertyMapping = new BooleanPropertyMapping(null, null, buildJsonPropertyMapping());
        Map<String, Object> map = Maps.newHashMap();
        for (String falseValue : falseValues) {
            map.put("0", falseValue);
            assertFalse((Boolean) propertyMapping.decodeValue(map));
        }
    }

    @Test
    public void testUnknownValue() throws Exception {
        BooleanPropertyMapping propertyMapping = new BooleanPropertyMapping(null, null, buildJsonPropertyMapping());

        Map<String, Object> row = createIndexedMap("maybe");

        try {
            propertyMapping.decodeValue(row);
            fail("An exception should have been raised");
        } catch (VisalloException ve) {
            assertEquals("Unrecognized boolean value: maybe", ve.getMessage());
        }
    }

    @Test
    public void testBlankValueWithNoDefault() throws Exception {
        BooleanPropertyMapping propertyMapping = new BooleanPropertyMapping(null, null, buildJsonPropertyMapping());

        Map<String, Object> row = createIndexedMap("    ");

        assertNull(propertyMapping.decodeValue(row));
    }

    @Test
    public void testBlankValueWithFalseDefault() throws Exception {
        JSONObject jsonPropertyMapping = buildJsonPropertyMapping();
        jsonPropertyMapping.put(BooleanPropertyMapping.PROPERTY_MAPPING_DEFAULT_KEY, "false");

        BooleanPropertyMapping propertyMapping = new BooleanPropertyMapping(null, null, jsonPropertyMapping);

        Map<String, Object> row = createIndexedMap("    ");
        assertFalse((Boolean) propertyMapping.decodeValue(row));
    }

    @Test
    public void testBlankValueWithTrueDefault() throws Exception {
        JSONObject jsonPropertyMapping = buildJsonPropertyMapping();
        jsonPropertyMapping.put(BooleanPropertyMapping.PROPERTY_MAPPING_DEFAULT_KEY, "true");

        BooleanPropertyMapping propertyMapping = new BooleanPropertyMapping(null, null, jsonPropertyMapping);

        Map<String, Object> row = createIndexedMap("    ");
        assertTrue((Boolean) propertyMapping.decodeValue(row));
    }

    private JSONObject buildJsonPropertyMapping() {
        JSONObject jsonProperyMapping = new JSONObject();
        jsonProperyMapping.put(PropertyMapping.PROPERTY_MAPPING_NAME_KEY, "JUNIT");
        jsonProperyMapping.put(PropertyMapping.PROPERTY_MAPPING_KEY_KEY, 0);
        jsonProperyMapping.put(BooleanPropertyMapping.PROPERTY_MAPPING_BOOLEAN_TRUE_KEY, trueValues);
        jsonProperyMapping.put(BooleanPropertyMapping.PROPERTY_MAPPING_BOOLEAN_FALSE_KEY, falseValues);
        return jsonProperyMapping;
    }
}
