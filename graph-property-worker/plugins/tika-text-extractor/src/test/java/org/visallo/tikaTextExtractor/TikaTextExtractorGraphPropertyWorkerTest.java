package org.visallo.tikaTextExtractor;

import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerTestSetupBase;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Metadata;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.VertexBuilder;
import org.vertexium.property.StreamingPropertyValue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class TikaTextExtractorGraphPropertyWorkerTest extends GraphPropertyWorkerTestSetupBase {

    @Override
    protected GraphPropertyWorker createGraphPropertyWorker() {
        return new TikaTextExtractorGraphPropertyWorker();
    }

    @Test
    public void testExtractWithHtml() throws Exception {
        String data = "<html>";
        data += "<head>";
        data += "<title>Test Title</title>";
        data += "<meta content=\"2013-01-01T18:09:20Z\" itemprop=\"datePublished\" name=\"pubdate\"/>";
        data += "</head>";
        data += "<body>";
        data += "<div><table><tr><td>Menu1</td><td>Menu2</td><td>Menu3</td></tr></table></div>\n";
        data += "\n";
        data += "<h1>Five reasons why Windows 8 has failed</h1>\n";
        data += "<p>The numbers speak for themselves. Vista, universally acknowledged as a failure, actually had significantly better adoption numbers than Windows 8. At similar points in their roll-outs, Vista had a desktop market share of 4.52% compared to Windows 8's share of 2.67%. Underlining just how poorly Windows 8's adoption has gone, Vista didn't even have the advantage of holiday season sales to boost its numbers. Tablets--and not Surface RT tablets--were what people bought last December, not Windows 8 PCs.</p>\n";
        data += "</body>";
        data += "</html>";
        createVertex(data, "text/html");

        InputStream in = new ByteArrayInputStream(data.getBytes());
        Vertex vertex = graph.getVertex("v1", authorizations);
        Property property = vertex.getProperty(VisalloProperties.RAW.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, property, null, null, Priority.NORMAL);
        worker.execute(in, workData);

        vertex = graph.getVertex("v1", authorizations);
        assertEquals("Test Title", VisalloProperties.TITLE.getOnlyPropertyValue(vertex));

        assertEquals(
                "Five reasons why Windows 8 has failed\n" +
                        "The numbers speak for themselves. Vista, universally acknowledged as a failure, actually had significantly better adoption numbers than Windows 8. At similar points in their roll-outs, Vista had a desktop market share of 4.52% compared to Windows 8's share of 2.67%. Underlining just how poorly Windows 8's adoption has gone, Vista didn't even have the advantage of holiday season sales to boost its numbers. Tablets--and not Surface RT tablets--were what people bought last December, not Windows 8 PCs.\n",
                IOUtils.toString(VisalloProperties.TEXT.getOnlyPropertyValue(vertex).getInputStream(), "UTF-8")
        );
        assertEquals(new Date(1357063760000L), VisalloProperties.MODIFIED_DATE.getPropertyValue(vertex));
    }

    private void createVertex(String data, String mimeType) throws UnsupportedEncodingException {
        VertexBuilder v = graph.prepareVertex("v1", visibility);
        StreamingPropertyValue textValue = new StreamingPropertyValue(new ByteArrayInputStream(data.getBytes("UTF-8")), byte[].class);
        textValue.searchIndex(false);
        Metadata metadata = new Metadata();
        metadata.add(VisalloProperties.MIME_TYPE.getPropertyName(), mimeType, visibilityTranslator.getDefaultVisibility());
        VisalloProperties.RAW.setProperty(v, textValue, metadata, visibility);
        v.save(authorizations);
    }

    @Test
    public void testExtractWithEmptyHtml() throws Exception {
        String data = "<html>";
        data += "<head>";
        data += "<title>Test Title</title>";
        data += "<meta content=\"2013-01-01T18:09:20Z\" itemprop=\"datePublished\" name=\"pubdate\"/>";
        data += "</head>";
        data += "<body>";
        data += "</body>";
        data += "</html>";
        createVertex(data, "text/html");

        InputStream in = new ByteArrayInputStream(data.getBytes());
        Vertex vertex = graph.getVertex("v1", authorizations);
        Property property = vertex.getProperty(VisalloProperties.RAW.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, property, null, null, Priority.LOW);
        worker.execute(in, workData);

        vertex = graph.getVertex("v1", authorizations);
        assertEquals("Test Title", VisalloProperties.TITLE.getOnlyPropertyValue(vertex));
        assertEquals("", IOUtils.toString(VisalloProperties.TEXT.getOnlyPropertyValue(vertex).getInputStream(), "UTF-8"));
        assertEquals(new Date(1357063760000L), VisalloProperties.MODIFIED_DATE.getPropertyValue(vertex));
    }

    @Test
    public void testExtractWithNotHtml() throws Exception {
        String data = "<title>Test Title</title>";
        data += "<meta content=\"2013-01-01T18:09:20Z\" itemprop=\"datePublished\" name=\"pubdate\"/>";
        data += "<h1>Five reasons why Windows 8 has failed</h1>";
        data += "<p>The numbers speak for themselves. Vista, universally acknowledged as a failure, actually had significantly better adoption numbers than Windows 8. At similar points in their roll-outs, Vista had a desktop market share of 4.52% compared to Windows 8's share of 2.67%. Underlining just how poorly Windows 8's adoption has gone, Vista didn't even have the advantage of holiday season sales to boost its numbers. Tablets--and not Surface RT tablets--were what people bought last December, not Windows 8 PCs.</p>";
        data += "</body>";
        data += "</html>";
        createVertex(data, "text/html");

        InputStream in = new ByteArrayInputStream(data.getBytes());
        Vertex vertex = graph.getVertex("v1", authorizations);
        Property property = vertex.getProperty(VisalloProperties.RAW.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, property, null, null, Priority.LOW);
        worker.execute(in, workData);

        vertex = graph.getVertex("v1", authorizations);
        assertEquals("Test Title", VisalloProperties.TITLE.getOnlyPropertyValue(vertex));
        assertEquals(
                "Five reasons why Windows 8 has failed\n" +
                        "The numbers speak for themselves. Vista, universally acknowledged as a failure, actually had significantly better adoption numbers than Windows 8. At similar points in their roll-outs, Vista had a desktop market share of 4.52% compared to Windows 8's share of 2.67%. Underlining just how poorly Windows 8's adoption has gone, Vista didn't even have the advantage of holiday season sales to boost its numbers. Tablets--and not Surface RT tablets--were what people bought last December, not Windows 8 PCs.\n",
                IOUtils.toString(VisalloProperties.TEXT.getOnlyPropertyValue(vertex).getInputStream(), "UTF-8")
        );
        assertEquals(new Date(1357063760000L), VisalloProperties.MODIFIED_DATE.getPropertyValue(vertex));
    }

    @Test
    public void testExtractTextWithAccentCharacters() throws Exception {
        String data = "the Quita Suena\u0301 bank";
        createVertex(data, "text/plain; charset=utf-8");

        InputStream in = new ByteArrayInputStream(data.getBytes("UTF-8"));
        Vertex vertex = graph.getVertex("v1", authorizations);
        Property property = vertex.getProperty(VisalloProperties.RAW.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(visibilityTranslator, vertex, property, null, null, Priority.LOW);
        worker.execute(in, workData);

        vertex = graph.getVertex("v1", authorizations);
        String expected = "the Quita Suená bank ";
        String actual = IOUtils.toString(VisalloProperties.TEXT.getOnlyPropertyValue(vertex).getInputStream(), "UTF-8");
        assertEquals(21, expected.length());
        assertEquals(expected, actual);
        assertEquals(expected.length(), actual.length());
    }

    //todo : add test with image metadata
}
