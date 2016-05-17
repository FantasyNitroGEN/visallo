package org.visallo.core.model.ontology;

import org.junit.Test;
import org.vertexium.type.GeoPoint;
import org.visallo.web.clientapi.model.PropertyType;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OntologyPropertyTest {
    @Test
    public void testConvertObject() throws ParseException {
        Date date = new Date();
        assertEquals(date, createOntologyProperty(PropertyType.DATE).convert(date));
        assertEquals(new GeoPoint(10, 20), createOntologyProperty(PropertyType.GEO_LOCATION).convert(new GeoPoint(10, 20)));
        assertEquals(new BigDecimal("1.23"), createOntologyProperty(PropertyType.CURRENCY).convert(new BigDecimal("1.23")));
        assertEquals(1.2345, createOntologyProperty(PropertyType.DOUBLE).convert(1.2345));
        assertEquals(123, createOntologyProperty(PropertyType.INTEGER).convert(123));
        assertEquals(true, createOntologyProperty(PropertyType.BOOLEAN).convert(true));
        assertEquals("test", createOntologyProperty(PropertyType.STRING).convert("test"));
    }

    @Test
    public void testConvertString() throws ParseException {
        assertEquals(
                OntologyProperty.DATE_TIME_WITH_SECONDS_FORMAT.parse("2016-03-15 04:12:13"),
                createOntologyProperty(PropertyType.DATE).convertString("2016-03-15 04:12:13")
        );
        assertEquals(new GeoPoint(10, 20), createOntologyProperty(PropertyType.GEO_LOCATION).convertString("POINT(10, 20)"));
        assertEquals(new BigDecimal("1.23"), createOntologyProperty(PropertyType.CURRENCY).convertString("1.23"));
        assertEquals(1.2345, createOntologyProperty(PropertyType.DOUBLE).convertString("1.2345"));
        assertEquals(123, createOntologyProperty(PropertyType.INTEGER).convertString("123"));
        assertEquals(true, createOntologyProperty(PropertyType.BOOLEAN).convertString("true"));
        assertEquals("test", createOntologyProperty(PropertyType.STRING).convertString("test"));
    }

    private OntologyProperty createOntologyProperty(PropertyType dataType) throws ParseException {
        OntologyProperty ontologyProperty = mock(OntologyProperty.class);
        when(ontologyProperty.getDataType()).thenReturn(dataType);
        when(ontologyProperty.convert(any(Object.class))).thenCallRealMethod();
        when(ontologyProperty.convertString(any(String.class))).thenCallRealMethod();
        return ontologyProperty;
    }
}