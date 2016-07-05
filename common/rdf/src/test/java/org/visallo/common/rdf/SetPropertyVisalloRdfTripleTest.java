package org.visallo.common.rdf;

import org.junit.Test;
import org.vertexium.DateOnly;
import org.vertexium.ElementType;
import org.vertexium.type.GeoPoint;
import org.visallo.web.clientapi.model.DirectoryPerson;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class SetPropertyVisalloRdfTripleTest extends VisalloRdfTripleTestBase {
    @Test
    public void testToStringVertex() {
        SetPropertyVisalloRdfTriple triple = new SetPropertyVisalloRdfTriple(
                ElementType.VERTEX,
                "v1", "A",
                "pkey", "http://visallo.org#pname", "B",
                "value1"
        );
        assertEqualsVisalloRdfTriple("<v1[A]> <http://visallo.org#pname:pkey[B]> \"value1\"", triple);
    }

    @Test
    public void testToStringVertexWithPropertyKeyThatHasAColon() {
        SetPropertyVisalloRdfTriple triple = new SetPropertyVisalloRdfTriple(
                ElementType.VERTEX,
                "v1", "A",
                "pkey:1", "http://visallo.org#pname", "B",
                "value1"
        );
        assertEqualsVisalloRdfTriple("<v1[A]> <http://visallo.org#pname:pkey\\:1[B]> \"value1\"", triple);
    }

    @Test
    public void testToStringEdge() {
        SetPropertyVisalloRdfTriple triple = new SetPropertyVisalloRdfTriple(
                ElementType.EDGE,
                "e1", "A",
                "pkey", "http://visallo.org#pname", "B",
                "value1"
        );
        assertEqualsVisalloRdfTriple("<EDGE:e1[A]> <http://visallo.org#pname:pkey[B]> \"value1\"", triple);
    }

    @Test
    public void testStringValue() {
        testValue("value1", "\"value1\"");
    }

    @Test
    public void testIntegerValue() {
        testValue(42, "\"42\"^^<http://www.w3.org/2001/XMLSchema#int>");
    }

    @Test
    public void testDoubleValue() {
        testValue(42.2, "\"42.2\"^^<http://www.w3.org/2001/XMLSchema#double>");
    }

    @Test
    public void testBooleanValue() {
        testValue(true, "\"true\"^^<http://www.w3.org/2001/XMLSchema#boolean>");
    }

    @Test
    public void testGeoLocationValue() {
        testValue(new GeoPoint(42.2, 54.6), "\"42.2, 54.6\"^^<http://visallo.org#geolocation>");
    }

    @Test
    public void testDateTimeValue() {
        Date date = Date.from(LocalDateTime.of(2016, 5, 19, 1, 46, 28).atZone(ZoneId.of("GMT")).toInstant());
        testValue(date, "\"2016-05-19T01:46:28Z\"^^<http://www.w3.org/2001/XMLSchema#dateTime>");
    }

    @Test
    public void testDirectoryEntryValue() {
        testValue(new DirectoryPerson("jferner", "Joe Ferner"), "\"jferner\"^^<http://visallo.org#directory/entity>", false);
    }

    @Test
    public void testDateValue() {
        testValue(new DateOnly(2016, 5, 19), "\"2016-06-19\"^^<http://www.w3.org/2001/XMLSchema#date>", false);
    }

    private void testValue(Object value, String expected) {
        testValue(value, expected, true);
    }

    private void testValue(Object value, String expected, boolean assertRoundTrip) {
        SetPropertyVisalloRdfTriple triple = new SetPropertyVisalloRdfTriple(
                ElementType.VERTEX,
                "v1", "A",
                "pkey", "http://visallo.org#pname", "B",
                value
        );
        String expectedString = "<v1[A]> <http://visallo.org#pname:pkey[B]> " + expected;
        assertEqualsVisalloRdfTriple(expectedString, triple, assertRoundTrip);
    }
}