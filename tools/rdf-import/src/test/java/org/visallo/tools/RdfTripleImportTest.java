package org.visallo.tools;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryGraph;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.type.GeoPoint;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.util.VisalloDate;
import org.visallo.core.util.VisalloDateTime;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.*;
import static org.vertexium.util.IterableUtils.toList;

public class RdfTripleImportTest {
    private RdfTripleImport rdfTripleImport;
    private InMemoryGraph graph;
    private Authorizations authorizations;
    private Metadata metadata;

    @Before
    public void setUp() {
        graph = InMemoryGraph.create();
        metadata = new Metadata();
        Visibility visibility = new Visibility("");
        authorizations = graph.createAuthorizations("A");
        TimeZone timeZone = TimeZone.getDefault();
        rdfTripleImport = new RdfTripleImport(graph, timeZone, visibility, authorizations);
        graph.addVertex("v1", new Visibility(""), authorizations);
        graph.addVertex("v2", new Visibility(""), authorizations);
        graph.flush();
    }

    @Test
    public void testImportConceptType() {
        rdfTripleImport.importRdfLine("<v1> <" + RdfTripleImport.LABEL_CONCEPT_TYPE + "> <http://visallo.org/test#type1>", metadata);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        assertEquals("http://visallo.org/test#type1", VisalloProperties.CONCEPT_TYPE.getPropertyValue(v1));
        assertNotNull(v1);
    }

    @Test
    public void testImportVertexWithVisibility() {
        Vertex v1 = graph.getVertex("v1", authorizations);
        graph.deleteVertex(v1, authorizations);
        graph.flush();

        rdfTripleImport.importRdfLine("<v1[A]> <" + RdfTripleImport.LABEL_CONCEPT_TYPE + "> <http://visallo.org/test#type1>", metadata);
        graph.flush();

        v1 = graph.getVertex("v1", authorizations);
        assertEquals(new VisalloVisibility("A").getVisibility().getVisibilityString(), v1.getVisibility().getVisibilityString());
        assertEquals("http://visallo.org/test#type1", VisalloProperties.CONCEPT_TYPE.getPropertyValue(v1));
        assertNotNull(v1);
    }

    @Test
    public void testImportProperty() {
        rdfTripleImport.importRdfLine("<v1> <http://visallo.org/test#prop1> \"hello world\"", metadata);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        assertEquals("hello world", v1.getPropertyValue(RdfTripleImport.MULTI_KEY, "http://visallo.org/test#prop1"));
    }

    @Test
    public void testImportDateProperty() {
        rdfTripleImport.importRdfLine("<v1> <http://visallo.org/test#prop1> \"2015-05-21\"^^<" + RdfTripleImport.PROPERTY_TYPE_DATE + ">", metadata);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        Date date = (Date) v1.getPropertyValue(RdfTripleImport.MULTI_KEY, "http://visallo.org/test#prop1");
        assertEquals(new VisalloDate(2015, 5, 21), VisalloDate.create(date));
        assertEquals("Date should be midnight in GMT: " + date, 1432166400000L, date.getTime());
    }

    @Test
    public void testImportDateTimeNoTimeZoneProperty() {
        rdfTripleImport.importRdfLine("<v1> <http://visallo.org/test#prop1> \"2015-05-21T08:42:22\"^^<" + RdfTripleImport.PROPERTY_TYPE_DATE_TIME + ">", metadata);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        VisalloDateTime dateTime = VisalloDateTime.create(v1.getPropertyValue(RdfTripleImport.MULTI_KEY, "http://visallo.org/test#prop1"));
        assertEquals(new VisalloDateTime(2015, 5, 21, 8, 42, 22, 0, TimeZone.getDefault().getID()), dateTime);

        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.setTimeInMillis(0);
        cal.set(2015, Calendar.MAY, 21, 8, 42, 22);
        assertEquals("Time incorrect: " + dateTime.toDate(TimeZone.getDefault()), cal.getTimeInMillis(), dateTime.getEpoch());
    }

    @Test
    public void testImportDateTimeWithGMTTimeZoneProperty() {
        rdfTripleImport.importRdfLine("<v1> <http://visallo.org/test#prop1> \"2015-05-21T08:42:22Z\"^^<" + RdfTripleImport.PROPERTY_TYPE_DATE_TIME + ">", metadata);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        VisalloDateTime dateTime = VisalloDateTime.create(v1.getPropertyValue(RdfTripleImport.MULTI_KEY, "http://visallo.org/test#prop1"));
        assertEquals(new VisalloDateTime(2015, 5, 21, 8, 42, 22, 0, "GMT"), dateTime);
        assertEquals("Time incorrect: " + dateTime.toDateGMT(), 1432197742000L, dateTime.getEpoch());
    }

    @Test
    public void testImportDateTimeWithESTTimeZoneProperty() {
        TimeZone tz = TimeZone.getTimeZone("America/Anchorage");
        String timeZoneOffset = "-0" + Math.abs(tz.getOffset(new VisalloDate(2015, Calendar.MAY, 21).getEpoch()) / 1000 / 60 / 60) + ":00";
        String line = "<v1> <http://visallo.org/test#prop1> \"2015-05-21T08:42:22" + timeZoneOffset + "\"^^<" + RdfTripleImport.PROPERTY_TYPE_DATE_TIME + ">";
        rdfTripleImport.importRdfLine(line, metadata);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        VisalloDateTime dateTime = VisalloDateTime.create(v1.getPropertyValue(RdfTripleImport.MULTI_KEY, "http://visallo.org/test#prop1"));
        assertEquals(new VisalloDateTime(2015, 5, 21, 8, 42, 22, 0, "America/Anchorage"), dateTime);
        assertEquals("Time incorrect: " + dateTime.toDateGMT(), 1432226542000L, dateTime.getEpoch());
    }

    @Test
    public void testImportGeoPoint() {
        String line = "<v1> <http://visallo.org/test#prop1> \"Dulles International Airport, VA [38.955589294433594, -77.44930267333984]\"^^<" + RdfTripleImport.PROPERTY_TYPE_GEOLOCATION + ">";
        rdfTripleImport.importRdfLine(line, metadata);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        GeoPoint prop1 = (GeoPoint) v1.getPropertyValue(RdfTripleImport.MULTI_KEY, "http://visallo.org/test#prop1");
        assertEquals("Dulles International Airport, VA", prop1.getDescription());
        assertEquals(38.955589294433594, prop1.getLatitude(), 0.00001);
        assertEquals(-77.44930267333984, prop1.getLongitude(), 0.00001);
    }

    @Test
    public void testImportPropertyWithKey() {
        rdfTripleImport.importRdfLine("<v1> <http://visallo.org/test#prop1:key1> \"hello world\"", metadata);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        assertEquals("hello world", v1.getPropertyValue("key1", "http://visallo.org/test#prop1"));
    }

    @Test
    public void testImportPropertyVisibility() {
        rdfTripleImport.importRdfLine("<v1> <http://visallo.org/test#prop1[A]> \"hello world\"", metadata);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        Property property = v1.getProperty("http://visallo.org/test#prop1");
        assertNotNull("Could not find property", property);
        assertEquals("hello world", property.getValue());
        assertEquals(new VisalloVisibility("A").getVisibility().getVisibilityString(), property.getVisibility().getVisibilityString());
    }

    @Test
    public void testImportPropertyVisibilityAndKey() {
        rdfTripleImport.importRdfLine("<v1> <http://visallo.org/test#prop1:key1[A]> \"hello world\"", metadata);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        Property property = v1.getProperty("key1", "http://visallo.org/test#prop1");
        assertNotNull("Could not find property with key", property);
        assertEquals("hello world", property.getValue());
        assertEquals(new VisalloVisibility("A").getVisibility().getVisibilityString(), property.getVisibility().getVisibilityString());
    }

    @Test
    public void testImportStreamingPropertyValue() throws IOException {
        File file = File.createTempFile(RdfTripleImportTest.class.getName(), "txt");
        file.deleteOnExit();

        FileUtils.writeStringToFile(file, "hello world");

        rdfTripleImport.importRdfLine("<v1> <http://visallo.org/test#prop1> \"" + file.getAbsolutePath() + "\"^^<" + RdfTripleImport.PROPERTY_TYPE_STREAMING_PROPERTY_VALUE + ">", metadata);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        Object propertyValue = v1.getPropertyValue(RdfTripleImport.MULTI_KEY, "http://visallo.org/test#prop1");
        assertTrue(propertyValue instanceof StreamingPropertyValue);
        assertEquals("hello world", IOUtils.toString(((StreamingPropertyValue) propertyValue).getInputStream()));
    }

    @Test
    public void testImportStreamingPropertyValueInline() throws IOException {
        rdfTripleImport.importRdfLine("<v1> <http://visallo.org/test#prop1> \"hello world\"^^<" + RdfTripleImport.PROPERTY_TYPE_STREAMING_PROPERTY_VALUE_INLINE + ">", metadata);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        Object propertyValue = v1.getPropertyValue(RdfTripleImport.MULTI_KEY, "http://visallo.org/test#prop1");
        assertTrue(propertyValue instanceof StreamingPropertyValue);
        assertEquals("hello world", IOUtils.toString(((StreamingPropertyValue) propertyValue).getInputStream()));
    }

    @Test
    public void testImportEdge() {
        rdfTripleImport.importRdfLine("<v1> <http://visallo.org/test#edgeLabel1> <v2>", metadata);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        assertEquals(1, v1.getEdgeCount(Direction.OUT, authorizations));
        List<Edge> edges = toList(v1.getEdges(Direction.OUT, authorizations));
        assertEquals(1, edges.size());
        assertEquals("http://visallo.org/test#edgeLabel1", edges.get(0).getLabel());
        assertEquals("v2", edges.get(0).getOtherVertex("v1", authorizations).getId());
    }

    @Test
    public void testImportEdgeWithVisibility() {
        rdfTripleImport.importRdfLine("<v1> <http://visallo.org/test#edgeLabel1[A]> <v2>", metadata);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        assertEquals(1, v1.getEdgeCount(Direction.OUT, authorizations));
        List<Edge> edges = toList(v1.getEdges(Direction.OUT, authorizations));
        assertEquals(1, edges.size());
        assertEquals(new VisalloVisibility("A").getVisibility().getVisibilityString(), edges.get(0).getVisibility().getVisibilityString());
        assertEquals("http://visallo.org/test#edgeLabel1", edges.get(0).getLabel());
        assertEquals("v2", edges.get(0).getOtherVertex("v1", authorizations).getId());
    }
}