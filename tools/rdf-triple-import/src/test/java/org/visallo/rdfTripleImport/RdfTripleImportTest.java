package org.visallo.rdfTripleImport;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryGraph;
import org.vertexium.property.StreamingPropertyValue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static org.vertexium.util.IterableUtils.toList;

public class RdfTripleImportTest {
    private RdfTripleImport rdfTripleImport;
    private InMemoryGraph graph;
    private Authorizations authorizations;

    @Before
    public void setUp() {
        graph = InMemoryGraph.create();
        Metadata metadata = new Metadata();
        Visibility visibility = new Visibility("");
        authorizations = graph.createAuthorizations();
        rdfTripleImport = new RdfTripleImport(graph, metadata, visibility, authorizations);
    }

    @Test
    public void testImportConceptType() {
        rdfTripleImport.importRdfLine("<v1> <" + RdfTripleImport.LABEL_CONCEPT_TYPE + "> <http://visallo.org/test#type1>");
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        assertNotNull(v1);
    }

    @Test
    public void testImportProperty() {
        graph.addVertex("v1", new Visibility(""), authorizations);
        graph.flush();

        rdfTripleImport.importRdfLine("<v1> <http://visallo.org/test#prop1> \"hello world\"");
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        assertEquals("hello world", v1.getPropertyValue(RdfTripleImport.MULTI_KEY, "http://visallo.org/test#prop1"));
    }

    @Test
    public void testImportStreamingPropertyValue() throws IOException {
        graph.addVertex("v1", new Visibility(""), authorizations);
        graph.flush();

        File file = File.createTempFile(RdfTripleImportTest.class.getName(), "txt");
        file.deleteOnExit();

        FileUtils.writeStringToFile(file, "hello world");

        rdfTripleImport.importRdfLine("<v1> <http://visallo.org/test#prop1> \"" + file.getAbsolutePath() + "\"^^<" + RdfTripleImport.PROPERTY_TYPE_STREAMING_PROPERTY_VALUE + ">");
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        Object propertyValue = v1.getPropertyValue(RdfTripleImport.MULTI_KEY, "http://visallo.org/test#prop1");
        assertTrue(propertyValue instanceof StreamingPropertyValue);
        assertEquals("hello world", IOUtils.toString(((StreamingPropertyValue) propertyValue).getInputStream()));
    }

    @Test
    public void testImportStreamingPropertyValueInline() throws IOException {
        graph.addVertex("v1", new Visibility(""), authorizations);
        graph.flush();

        rdfTripleImport.importRdfLine("<v1> <http://visallo.org/test#prop1> \"hello world\"^^<" + RdfTripleImport.PROPERTY_TYPE_STREAMING_PROPERTY_VALUE_INLINE + ">");
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        Object propertyValue = v1.getPropertyValue(RdfTripleImport.MULTI_KEY, "http://visallo.org/test#prop1");
        assertTrue(propertyValue instanceof StreamingPropertyValue);
        assertEquals("hello world", IOUtils.toString(((StreamingPropertyValue) propertyValue).getInputStream()));
    }

    @Test
    public void testImportEdge() {
        graph.addVertex("v1", new Visibility(""), authorizations);
        graph.addVertex("v2", new Visibility(""), authorizations);
        graph.flush();

        rdfTripleImport.importRdfLine("<v1> <http://visallo.org/test#edgeLabel1> <v2>");
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        assertEquals(1, v1.getEdgeCount(Direction.OUT, authorizations));
        List<Edge> edges = toList(v1.getEdges(Direction.OUT, authorizations));
        assertEquals(1, edges.size());
        assertEquals("http://visallo.org/test#edgeLabel1", edges.get(0).getLabel());
        assertEquals("v2", edges.get(0).getOtherVertex("v1", authorizations).getId());
    }
}