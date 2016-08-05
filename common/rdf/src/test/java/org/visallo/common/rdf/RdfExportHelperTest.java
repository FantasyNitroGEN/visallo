package org.visallo.common.rdf;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryGraph;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class RdfExportHelperTest {
    private InMemoryGraph graph;
    private Authorizations authorizations;
    private RdfExportHelper rdfExportHelper;

    @Before
    public void before() {
        graph = InMemoryGraph.create();
        authorizations = graph.createAuthorizations("A", "B");
        rdfExportHelper = new RdfExportHelper();
    }

    @Test
    public void testExportVertex() throws IOException {
        String warning = "\"Unhandled value type org.vertexium.inmemory.InMemoryStreamingPropertyValue to convert to RDF string\"";
        StreamingPropertyValue raw = new StreamingPropertyValue(IOUtils.toInputStream("abc", "UTF-8"), byte[].class);
        raw.searchIndex(false);
        VertexBuilder v1Builder = graph.prepareVertex("v1", new Visibility(""));
        VisalloProperties.VISIBILITY_JSON.setProperty(v1Builder, new VisibilityJson(""), new Visibility(""));
        VisalloProperties.CONCEPT_TYPE.setProperty(v1Builder, "http://visallo.org#person", new Visibility(""));
        Metadata metadata = new Metadata();
        metadata.add("meta1", "meta1Value", new Visibility(""));
        VisalloProperties.RAW.setProperty(v1Builder, raw, metadata, new Visibility(""));
        v1Builder.addPropertyValue("k1", "http://visallo.org#firstName", "Joe", metadata, new Visibility(""));
        v1Builder.save(authorizations);
        graph.flush();

        Vertex v1 = graph.getVertex("v1", authorizations);
        String rdf = rdfExportHelper.exportElementToRdfTriple(v1);
        String expected = "# Vertex: v1\n" +
                "<v1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://visallo.org#person>\n" +
                "<v1> <http://visallo.org#conceptType> \"http://visallo.org#person\"\n" +
                "<v1> <http://visallo.org#firstName:k1> \"Joe\"\n" +
                "<v1> <http://visallo.org#firstName:k1@meta1> \"meta1Value\"\n" +
                "# <v1> <http://visallo.org#raw> " + warning + "\n" +
                "<v1> <http://visallo.org#visibilityJson> \"{\\\"source\\\":\\\"\\\"}\"\n";
        assertEquals(expected, rdf);
    }

    @Test
    public void testExportEdge() {
        graph.addVertex("v1", new Visibility(""), authorizations);
        graph.addVertex("v2", new Visibility(""), authorizations);
        EdgeBuilderByVertexId e1Builder = graph.prepareEdge("e1", "v1", "v2", "http://visallo.org#knows", new Visibility(""));
        VisalloProperties.VISIBILITY_JSON.setProperty(e1Builder, new VisibilityJson(""), new Visibility(""));
        VisalloProperties.CONCEPT_TYPE.setProperty(e1Builder, "http://visallo.org#person", new Visibility(""));
        Metadata metadata = new Metadata();
        metadata.add("meta1", "meta1Value", new Visibility(""));
        e1Builder.addPropertyValue("k1", "http://visallo.org#firstName", "Joe", metadata, new Visibility(""));
        e1Builder.save(authorizations);
        graph.flush();

        Edge e1 = graph.getEdge("e1", authorizations);
        String rdf = rdfExportHelper.exportElementToRdfTriple(e1);
        String expected = "# Edge: e1\n" +
                "<v1> <http://visallo.org#knows:e1> <v2>\n" +
                "<EDGE:e1> <http://visallo.org#conceptType> \"http://visallo.org#person\"\n" +
                "<EDGE:e1> <http://visallo.org#firstName:k1> \"Joe\"\n" +
                "<EDGE:e1> <http://visallo.org#firstName:k1@meta1> \"meta1Value\"\n" +
                "<EDGE:e1> <http://visallo.org#visibilityJson> \"{\\\"source\\\":\\\"\\\"}\"\n";
        assertEquals(expected, rdf);
    }
}