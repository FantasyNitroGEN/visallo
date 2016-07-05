package org.visallo.common.rdf;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryGraph;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.type.GeoPoint;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.DirectVisibilityTranslator;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloDate;
import org.visallo.core.util.VisalloDateTime;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.vertexium.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class RdfTripleImportHelperTest {
    private RdfTripleImportHelper rdfTripleImportHelper;
    private InMemoryGraph graph;
    private Authorizations authorizations;
    private TimeZone timeZone;
    private File workingDir;
    private VisibilityTranslator visibilityTranslator;

    @Mock
    private WorkQueueRepository workQueueRepository;

    @Mock
    private User user;
    private String defaultVisibilitySource;
    private String sourceFileName;

    @Before
    public void setUp() {
        graph = InMemoryGraph.create();
        workingDir = new File(".");
        authorizations = graph.createAuthorizations("A");
        timeZone = TimeZone.getDefault();
        visibilityTranslator = new DirectVisibilityTranslator();

        rdfTripleImportHelper = new RdfTripleImportHelper(graph, visibilityTranslator, workQueueRepository);
        defaultVisibilitySource = "";
        sourceFileName = "test.nt";
        graph.addVertex("v1", new Visibility(""), authorizations);
        graph.addVertex("v2", new Visibility(""), authorizations);
        graph.flush();
    }

    @Test
    public void testImportConceptType() {
        String line = "<v1> <" + VisalloRdfTriple.LABEL_CONCEPT_TYPE + "> <http://visallo.org/test#type1>";
        importRdfLine(line);
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

        String line = "<v1[A]> <" + VisalloRdfTriple.LABEL_CONCEPT_TYPE + "> <http://visallo.org/test#type1>";
        importRdfLine(line);
        graph.flush();

        v1 = graph.getVertex("v1", authorizations);
        assertEquals(
                new VisalloVisibility("(A)").getVisibility().getVisibilityString(),
                v1.getVisibility().getVisibilityString()
        );
        assertEquals("http://visallo.org/test#type1", VisalloProperties.CONCEPT_TYPE.getPropertyValue(v1));
        assertEquals(new VisibilityJson("A"), VisalloProperties.VISIBILITY_JSON.getPropertyValue(v1));
        assertNotNull(v1);
    }

    @Test
    public void testImportProperty() {
        String line = "<v1> <http://visallo.org/test#prop1> \"hello world\"";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        Property property = v1.getProperty(VisalloRdfTriple.MULTI_KEY, "http://visallo.org/test#prop1");
        assertEquals("hello world", property.getValue());
        assertEquals(
                new VisalloVisibility("").getVisibility().getVisibilityString(),
                property.getVisibility().getVisibilityString()
        );
        assertEquals(
                new VisibilityJson(""),
                VisalloProperties.VISIBILITY_JSON_METADATA.getMetadataValue(property.getMetadata())
        );
    }

    @Test
    public void testImportPropertyWithNonBlankDefaultVisibility() {
        defaultVisibilitySource = "A";
        String line = "<v1> <http://visallo.org/test#prop1> \"hello world\"";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        Property property = v1.getProperty(VisalloRdfTriple.MULTI_KEY, "http://visallo.org/test#prop1");
        assertEquals("hello world", property.getValue());
        assertEquals(
                new VisalloVisibility("(A)").getVisibility().getVisibilityString(),
                property.getVisibility().getVisibilityString()
        );
        assertEquals(
                new VisibilityJson("A"),
                VisalloProperties.VISIBILITY_JSON_METADATA.getMetadataValue(property.getMetadata())
        );
    }

    @Test
    public void testImportPropertyMetadata() {
        importRdfLine("<v1> <http://visallo.org/test#prop1> \"hello world\"");
        importRdfLine("<v1> <http://visallo.org/test#prop1@metadata\\@1> \"metadata value 1\"");
        importRdfLine("<v1> <http://visallo.org/test#prop1@metadata2> \"metadata value 2\"");
        importRdfLine("<v1> <http://visallo.org/test#prop1@metadata2[S]> \"metadata value 2 S\"");
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        Property prop1 = v1.getProperty(VisalloRdfTriple.MULTI_KEY, "http://visallo.org/test#prop1");
        assertEquals("hello world", prop1.getValue());
        assertEquals("metadata value 1", prop1.getMetadata().getValue("metadata@1"));
        assertEquals("metadata value 2", prop1.getMetadata().getValue("metadata2", new Visibility("")));
        assertEquals("metadata value 2 S", prop1.getMetadata().getValue("metadata2", new Visibility("((S))|visallo")));
    }

    @Test
    public void testImportDateProperty() {
        String line = "<v1> <http://visallo.org/test#prop1> \"2015-05-21\"^^<" + VisalloRdfTriple.PROPERTY_TYPE_DATE + ">";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        Date date = (Date) v1.getPropertyValue(VisalloRdfTriple.MULTI_KEY, "http://visallo.org/test#prop1");
        assertEquals(new VisalloDate(2015, 5, 21), VisalloDate.create(date));
        assertEquals("Date should be midnight in GMT: " + date, 1432166400000L, date.getTime());
    }

    @Test
    public void testImportDateTimeNoTimeZoneProperty() {
        String line = "<v1> <http://visallo.org/test#prop1> \"2015-05-21T08:42:22\"^^<" + VisalloRdfTriple.PROPERTY_TYPE_DATE_TIME + ">";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        VisalloDateTime dateTime = VisalloDateTime.create(v1.getPropertyValue(
                VisalloRdfTriple.MULTI_KEY,
                "http://visallo.org/test#prop1"
        ));
        assertEquals(new VisalloDateTime(2015, 5, 21, 8, 42, 22, 0, TimeZone.getDefault().getID()), dateTime);

        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.setTimeInMillis(0);
        cal.set(2015, Calendar.MAY, 21, 8, 42, 22);
        assertEquals(
                "Time incorrect: " + dateTime.toDate(TimeZone.getDefault()),
                cal.getTimeInMillis(),
                dateTime.getEpoch()
        );
    }

    @Test
    public void testImportDateTimeWithGMTTimeZoneProperty() {
        String line = "<v1> <http://visallo.org/test#prop1> \"2015-05-21T08:42:22Z\"^^<" + VisalloRdfTriple.PROPERTY_TYPE_DATE_TIME + ">";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        VisalloDateTime dateTime = VisalloDateTime.create(v1.getPropertyValue(
                VisalloRdfTriple.MULTI_KEY,
                "http://visallo.org/test#prop1"
        ));
        assertEquals(new VisalloDateTime(2015, 5, 21, 8, 42, 22, 0, "GMT"), dateTime);
        assertEquals("Time incorrect: " + dateTime.toDateGMT(), 1432197742000L, dateTime.getEpoch());
    }

    @Test
    public void testImportDateTimeWithESTTimeZoneProperty() {
        TimeZone tz = TimeZone.getTimeZone("America/Anchorage");
        String timeZoneOffset = "-0" + Math.abs(tz.getOffset(new VisalloDate(
                2015,
                Calendar.MAY,
                21
        ).getEpoch()) / 1000 / 60 / 60) + ":00";
        String line = "<v1> <http://visallo.org/test#prop1> \"2015-05-21T08:42:22" + timeZoneOffset + "\"^^<" + VisalloRdfTriple.PROPERTY_TYPE_DATE_TIME + ">";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        VisalloDateTime dateTime = VisalloDateTime.create(v1.getPropertyValue(
                VisalloRdfTriple.MULTI_KEY,
                "http://visallo.org/test#prop1"
        ));
        assertEquals(new VisalloDateTime(2015, 5, 21, 8, 42, 22, 0, "America/Anchorage"), dateTime);
        assertEquals("Time incorrect: " + dateTime.toDateGMT(), 1432226542000L, dateTime.getEpoch());
    }

    @Test
    public void testImportGeoPoint() {
        String line = "<v1> <http://visallo.org/test#prop1> \"Dulles International Airport, VA [38.955589294433594, -77.44930267333984]\"^^<" + VisalloRdfTriple.PROPERTY_TYPE_GEOLOCATION + ">";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        GeoPoint prop1 = (GeoPoint) v1.getPropertyValue(VisalloRdfTriple.MULTI_KEY, "http://visallo.org/test#prop1");
        assertEquals("Dulles International Airport, VA", prop1.getDescription());
        assertEquals(38.955589294433594, prop1.getLatitude(), 0.00001);
        assertEquals(-77.44930267333984, prop1.getLongitude(), 0.00001);
    }

    @Test
    public void testImportDirectoryEntity() {
        String line = "<v1> <http://visallo.org/test#prop1> \"joe\"^^<" + VisalloRdfTriple.PROPERTY_TYPE_DIRECTORY_ENTITY + ">";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        String prop1 = (String) v1.getPropertyValue(VisalloRdfTriple.MULTI_KEY, "http://visallo.org/test#prop1");
        assertEquals("joe", prop1);
    }

    @Test
    public void testImportPropertyWithKey() {
        String line = "<v1> <http://visallo.org/test#prop1:key1> \"hello world\"";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        assertEquals("hello world", v1.getPropertyValue("key1", "http://visallo.org/test#prop1"));
    }

    @Test
    public void testImportPropertyWithKeyThatHasAColon() {
        String line = "<v1> <http://visallo.org/test#prop1:key\\:1> \"hello world\"";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        assertEquals("hello world", v1.getPropertyValue("key:1", "http://visallo.org/test#prop1"));
    }

    @Test
    public void testImportPropertyVisibility() {
        String line = "<v1> <http://visallo.org/test#prop1[A]> \"hello world\"";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        Property property = v1.getProperty("http://visallo.org/test#prop1");
        assertNotNull("Could not find property", property);
        assertEquals("hello world", property.getValue());
        assertEquals(
                new VisalloVisibility("(A)").getVisibility().getVisibilityString(),
                property.getVisibility().getVisibilityString()
        );
        assertEquals(
                new VisibilityJson("A"),
                VisalloProperties.VISIBILITY_JSON_METADATA.getMetadataValue(property.getMetadata())
        );
    }

    @Test
    public void testImportPropertyVisibilityAndKey() {
        String line = "<v1> <http://visallo.org/test#prop1:key1[A]> \"hello world\"";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        Property property = v1.getProperty("key1", "http://visallo.org/test#prop1");
        assertNotNull("Could not find property with key", property);
        assertEquals("hello world", property.getValue());
        assertEquals(
                new VisalloVisibility("(A)").getVisibility().getVisibilityString(),
                property.getVisibility().getVisibilityString()
        );
        assertEquals(
                new VisibilityJson("A"),
                VisalloProperties.VISIBILITY_JSON_METADATA.getMetadataValue(property.getMetadata())
        );
    }

    @Test
    public void testImportStreamingPropertyValue() throws IOException {
        File file = File.createTempFile(RdfTripleImportHelperTest.class.getName(), "txt");
        file.deleteOnExit();

        FileUtils.writeStringToFile(file, "hello world");

        String line = "<v1> <http://visallo.org/test#prop1> \"" + file.getAbsolutePath() + "\"^^<" + VisalloRdfTriple.PROPERTY_TYPE_STREAMING_PROPERTY_VALUE + ">";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        Object propertyValue = v1.getPropertyValue(VisalloRdfTriple.MULTI_KEY, "http://visallo.org/test#prop1");
        assertTrue(propertyValue instanceof StreamingPropertyValue);
        assertEquals("hello world", IOUtils.toString(((StreamingPropertyValue) propertyValue).getInputStream()));
    }

    @Test
    public void testImportStreamingPropertyValueInline() throws IOException {
        String line = "<v1> <http://visallo.org/test#prop1> \"hello world\"^^<" + VisalloRdfTriple.PROPERTY_TYPE_STREAMING_PROPERTY_VALUE_INLINE + ">";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        Object propertyValue = v1.getPropertyValue(VisalloRdfTriple.MULTI_KEY, "http://visallo.org/test#prop1");
        assertTrue(propertyValue instanceof StreamingPropertyValue);
        assertEquals("hello world", IOUtils.toString(((StreamingPropertyValue) propertyValue).getInputStream()));
    }

    @Test
    public void testImportEdge() {
        String line = "<v1> <http://visallo.org/test#edgeLabel1> <v2>";
        importRdfLine(line);
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
        String line = "<v1> <http://visallo.org/test#edgeLabel1[A]> <v2>";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        assertEquals(1, v1.getEdgeCount(Direction.OUT, authorizations));
        List<Edge> edges = toList(v1.getEdges(Direction.OUT, authorizations));
        assertEquals(1, edges.size());
        assertEquals(
                new VisalloVisibility("(A)").getVisibility().getVisibilityString(),
                edges.get(0).getVisibility().getVisibilityString()
        );
        assertEquals("http://visallo.org/test#edgeLabel1", edges.get(0).getLabel());
        assertEquals("v2", edges.get(0).getOtherVertex("v1", authorizations).getId());
    }

    @Test
    public void testImportEdgeWithId() {
        String line = "<v1> <http://visallo.org/test#edgeLabel1:edge1> <v2>";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        assertEquals(1, v1.getEdgeCount(Direction.OUT, authorizations));
        List<Edge> edges = toList(v1.getEdges(Direction.OUT, authorizations));
        assertEquals(1, edges.size());
        assertEquals("http://visallo.org/test#edgeLabel1", edges.get(0).getLabel());
        assertEquals("v2", edges.get(0).getOtherVertex("v1", authorizations).getId());
        assertEquals("edge1", edges.get(0).getId());
    }

    @Test
    public void testImportEdgeWithIdWithAColon() {
        String line = "<v1> <http://visallo.org/test#edgeLabel1:edge\\:1> <v2>";
        importRdfLine(line);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        assertEquals(1, v1.getEdgeCount(Direction.OUT, authorizations));
        List<Edge> edges = toList(v1.getEdges(Direction.OUT, authorizations));
        assertEquals(1, edges.size());
        assertEquals("http://visallo.org/test#edgeLabel1", edges.get(0).getLabel());
        assertEquals("v2", edges.get(0).getOtherVertex("v1", authorizations).getId());
        assertEquals("edge:1", edges.get(0).getId());
    }

    @Test
    public void testImportEdgeProperty() {
        graph.addEdge("edge1", "v1", "v2", "label1", new Visibility(""), authorizations);

        String line = "<EDGE:edge1> <http://visallo.org/test#prop1> \"hello world\"";
        importRdfLine(line);
        graph.flush();

        Edge edge1 = graph.getEdge("edge1", authorizations);
        Property property = edge1.getProperty(VisalloRdfTriple.MULTI_KEY, "http://visallo.org/test#prop1");
        assertEquals("hello world", property.getValue());
        assertEquals(
                new VisalloVisibility("").getVisibility().getVisibilityString(),
                property.getVisibility().getVisibilityString()
        );
        assertEquals(
                new VisibilityJson(""),
                VisalloProperties.VISIBILITY_JSON_METADATA.getMetadataValue(property.getMetadata())
        );
    }

    private void importRdfLine(String line) {
        Set<Element> elements = new HashSet<>();
        rdfTripleImportHelper.importRdfLine(
                elements,
                sourceFileName,
                line,
                workingDir,
                timeZone,
                defaultVisibilitySource,
                user,
                authorizations
        );
    }
}