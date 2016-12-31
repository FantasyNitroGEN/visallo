package org.visallo.web.structuredingest.mapping;

import org.json.JSONObject;
import org.junit.Test;
import org.visallo.web.structuredingest.core.util.mapping.NumericPropertyMapping;
import org.visallo.web.structuredingest.core.util.mapping.PropertyMapping;
import org.visallo.vertexium.model.ontology.InMemoryOntologyProperty;
import org.visallo.web.clientapi.model.PropertyType;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

public class NumericPropertyMappingTest {
    @Test
    public void testCurrency() throws Exception {
        InMemoryOntologyProperty ontologyProperty = new InMemoryOntologyProperty();
        ontologyProperty.setDataType(PropertyType.CURRENCY);

        NumericPropertyMapping propertyMapping = new NumericPropertyMapping(ontologyProperty, null, null, buildJsonPropertyMapping());

        assertEquals(1420.45, propertyMapping.decodeValue("$1,420.45"));
        assertEquals(1420.45, propertyMapping.decodeValue("$1420.45"));
        assertEquals(1420.45, propertyMapping.decodeValue("1420.45"));
        assertEquals(1420.45, propertyMapping.decodeValue("1420.45 dollars"));
        assertEquals(-1420.45, propertyMapping.decodeValue("-1420.45"));
    }

    @Test
    public void testInteger() throws Exception {
        InMemoryOntologyProperty ontologyProperty = new InMemoryOntologyProperty();
        ontologyProperty.setDataType(PropertyType.INTEGER);

        NumericPropertyMapping propertyMapping = new NumericPropertyMapping(ontologyProperty, null, null, buildJsonPropertyMapping());

        assertEquals(1420L, propertyMapping.decodeValue("$1,420.45"));
        assertEquals(1420L, propertyMapping.decodeValue("$1420.45"));
        assertEquals(1420L, propertyMapping.decodeValue("1420.45"));
        assertEquals(1420L, propertyMapping.decodeValue("1420.45 dollars"));
        assertEquals(-1420L, propertyMapping.decodeValue("-1420.45"));
        assertEquals(1420L, propertyMapping.decodeValue("1,420"));
    }

    @Test
    public void testDouble() throws Exception {
        InMemoryOntologyProperty ontologyProperty = new InMemoryOntologyProperty();
        ontologyProperty.setDataType(PropertyType.DOUBLE);

        NumericPropertyMapping propertyMapping = new NumericPropertyMapping(ontologyProperty, null, null, buildJsonPropertyMapping());

        assertEquals(1420.45, propertyMapping.decodeValue("$1,420.45"));
        assertEquals(1420.45, propertyMapping.decodeValue("$1420.45"));
        assertEquals(1420.45, propertyMapping.decodeValue("1420.45"));
        assertEquals(1420.45, propertyMapping.decodeValue("1420.45 dollars"));
        assertEquals(-1420.45, propertyMapping.decodeValue("-1420.45"));
    }

    @Test
    public void testBlank() throws Exception {
        InMemoryOntologyProperty ontologyProperty = new InMemoryOntologyProperty();
        ontologyProperty.setDataType(PropertyType.DOUBLE);

        NumericPropertyMapping propertyMapping = new NumericPropertyMapping(ontologyProperty, null, null, buildJsonPropertyMapping());

        assertNull(propertyMapping.decodeValue("    "));
    }

    @Test
    public void testNonNumeric() throws Exception {
        InMemoryOntologyProperty ontologyProperty = new InMemoryOntologyProperty();
        ontologyProperty.setDataType(PropertyType.DOUBLE);

        NumericPropertyMapping propertyMapping = new NumericPropertyMapping(ontologyProperty, null, null, buildJsonPropertyMapping());

        assertNull(propertyMapping.decodeValue("boston"));
    }

    @Test
    public void testMalformedNumber() throws Exception {
        InMemoryOntologyProperty ontologyProperty = new InMemoryOntologyProperty();
        ontologyProperty.setDataType(PropertyType.DOUBLE);

        NumericPropertyMapping propertyMapping = new NumericPropertyMapping(ontologyProperty, null, null, buildJsonPropertyMapping());

        assertEquals(703555.71234, propertyMapping.decodeValue("aa (703)555.7-1234"));
        assertEquals(-703555.71234, propertyMapping.decodeValue("aa -(703)555.7-1234"));
    }

    private JSONObject buildJsonPropertyMapping() {
        JSONObject jsonProperyMapping = new JSONObject();
        jsonProperyMapping.put(PropertyMapping.PROPERTY_MAPPING_NAME_KEY, "JUNIT");
        jsonProperyMapping.put(PropertyMapping.PROPERTY_MAPPING_KEY_KEY, 0);
        return jsonProperyMapping;
    }
}
