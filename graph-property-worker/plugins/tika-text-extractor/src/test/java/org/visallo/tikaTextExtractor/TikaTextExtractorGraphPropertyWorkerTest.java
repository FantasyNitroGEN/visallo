package org.visallo.tikaTextExtractor;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.test.GraphPropertyWorkerTestBase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TikaTextExtractorGraphPropertyWorkerTest extends GraphPropertyWorkerTestBase {
    private static final String DOCUMENT_TITLE_PROPERTY_IRI = "http://visallo.org/test#title";
    private TikaTextExtractorGraphPropertyWorker gpw;
    private Visibility visibility;

    @Before
    public void before() throws Exception {
        when(ontologyRepository.getPropertyIRIByIntent("documentTitle")).thenReturn(DOCUMENT_TITLE_PROPERTY_IRI);

        Configuration config = new HashMapConfigurationLoader(getConfigurationMap()).createConfiguration();
        TikaTextExtractorGraphPropertyWorkerConfiguration tikaConfig = new TikaTextExtractorGraphPropertyWorkerConfiguration(config);
        gpw = new TikaTextExtractorGraphPropertyWorker(tikaConfig);
        prepare(gpw);

        visibility = new Visibility("");
    }

    @Override
    protected Map getConfigurationMap() {
        Map result = super.getConfigurationMap();

        result.put(TikaTextExtractorGraphPropertyWorkerConfiguration.TEXT_EXTRACT_MAPPING_CONFIGURATION_PREFIX + ".text1.rawPropertyName", "http://visallo.org/test#raw1");
        result.put(TikaTextExtractorGraphPropertyWorkerConfiguration.TEXT_EXTRACT_MAPPING_CONFIGURATION_PREFIX + ".text1.extractedTextPropertyName", "http://visallo.org/test#text1");
        result.put(TikaTextExtractorGraphPropertyWorkerConfiguration.TEXT_EXTRACT_MAPPING_CONFIGURATION_PREFIX + ".text1.textDescription", "Text 1");

        result.put(TikaTextExtractorGraphPropertyWorkerConfiguration.TEXT_EXTRACT_MAPPING_CONFIGURATION_PREFIX + ".text2.rawPropertyName", "http://visallo.org/test#raw2");
        result.put(TikaTextExtractorGraphPropertyWorkerConfiguration.TEXT_EXTRACT_MAPPING_CONFIGURATION_PREFIX + ".text2.extractedTextPropertyName", "http://visallo.org/test#text2");

        return result;
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
        Vertex vertex = getGraph().getVertex("v1", getGraphAuthorizations());
        Property property = vertex.getProperty(VisalloProperties.RAW.getPropertyName());
        GraphPropertyWorkData workData = new GraphPropertyWorkData(getVisibilityTranslator(), vertex, property, null, null, Priority.NORMAL);
        gpw.execute(in, workData);

        vertex = getGraph().getVertex("v1", getGraphAuthorizations());
        assertEquals("Test Title", vertex.getPropertyValue(DOCUMENT_TITLE_PROPERTY_IRI));

        assertEquals(
                "Five reasons why Windows 8 has failed\n" +
                        "The numbers speak for themselves. Vista, universally acknowledged as a failure, actually had significantly better adoption numbers than Windows 8. At similar points in their roll-outs, Vista had a desktop market share of 4.52% compared to Windows 8's share of 2.67%. Underlining just how poorly Windows 8's adoption has gone, Vista didn't even have the advantage of holiday season sales to boost its numbers. Tablets--and not Surface RT tablets--were what people bought last December, not Windows 8 PCs.\n",
                IOUtils.toString(VisalloProperties.TEXT.getOnlyPropertyValue(vertex).getInputStream(), "UTF-8")
        );
        assertEquals(new Date(1357063760000L), VisalloProperties.MODIFIED_DATE.getPropertyValue(vertex));
    }

    private void createVertex(String data, String mimeType) throws UnsupportedEncodingException {
        VertexBuilder v = getGraph().prepareVertex("v1", visibility);
        StreamingPropertyValue textValue = new StreamingPropertyValue(new ByteArrayInputStream(data.getBytes("UTF-8")), byte[].class);
        textValue.searchIndex(false);
        Metadata metadata = new Metadata();
        metadata.add(VisalloProperties.MIME_TYPE.getPropertyName(), mimeType, getVisibilityTranslator().getDefaultVisibility());
        VisalloProperties.RAW.setProperty(v, textValue, metadata, visibility);
        v.save(getGraphAuthorizations());
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
        Vertex vertex = getGraph().getVertex("v1", getGraphAuthorizations());
        Property property = vertex.getProperty(VisalloProperties.RAW.getPropertyName());
        run(gpw, getWorkerPrepareData(), vertex, property, in);

        vertex = getGraph().getVertex("v1", getGraphAuthorizations());
        assertEquals("Test Title", vertex.getPropertyValue(DOCUMENT_TITLE_PROPERTY_IRI));
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
        Vertex vertex = getGraph().getVertex("v1", getGraphAuthorizations());
        Property property = vertex.getProperty(VisalloProperties.RAW.getPropertyName());
        run(gpw, getWorkerPrepareData(), vertex, property, in);

        vertex = getGraph().getVertex("v1", getGraphAuthorizations());
        assertEquals("Test Title", vertex.getPropertyValue(DOCUMENT_TITLE_PROPERTY_IRI));
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
        Vertex vertex = getGraph().getVertex("v1", getGraphAuthorizations());
        Property property = vertex.getProperty(VisalloProperties.RAW.getPropertyName());
        run(gpw, getWorkerPrepareData(), vertex, property, in);

        vertex = getGraph().getVertex("v1", getGraphAuthorizations());
        String expected = "the Quita Suená bank ";
        String actual = IOUtils.toString(VisalloProperties.TEXT.getOnlyPropertyValue(vertex).getInputStream(), "UTF-8");
        assertEquals(21, expected.length());
        assertEquals(expected, actual);
        assertEquals(expected.length(), actual.length());
    }

    @Test
    public void testDifferentKey() throws UnsupportedEncodingException {
        VertexBuilder v = getGraph().prepareVertex("v1", visibility);

        StreamingPropertyValue textValue = new StreamingPropertyValue(new ByteArrayInputStream("<html><body>Text1</body></html>".getBytes("UTF-8")), byte[].class);
        textValue.searchIndex(false);
        Metadata metadata = new Metadata();
        metadata.add(VisalloProperties.MIME_TYPE.getPropertyName(), "text/html", getVisibilityTranslator().getDefaultVisibility());
        v.addPropertyValue("key1", "http://visallo.org/test#raw1", textValue, metadata, visibility);

        v.save(getGraphAuthorizations());

        Vertex vertex = getGraph().getVertex("v1", getGraphAuthorizations());
        run(gpw, getWorkerPrepareData(), vertex);

        Property text1Property = vertex.getProperty("key1", "http://visallo.org/test#text1");
        assertTrue(text1Property != null);
        assertThat(((StreamingPropertyValue) text1Property.getValue()).readToString(), equalTo("Text1"));
    }

    @Test
    public void testMultipleRaws() throws UnsupportedEncodingException {
        VertexBuilder v = getGraph().prepareVertex("v1", visibility);

        StreamingPropertyValue textValue = new StreamingPropertyValue(new ByteArrayInputStream("<html><body>Text1</body></html>".getBytes("UTF-8")), byte[].class);
        textValue.searchIndex(false);
        Metadata metadata = new Metadata();
        metadata.add(VisalloProperties.MIME_TYPE.getPropertyName(), "text/html", getVisibilityTranslator().getDefaultVisibility());
        v.setProperty("http://visallo.org/test#raw1", textValue, metadata, visibility);

        textValue = new StreamingPropertyValue(new ByteArrayInputStream("<html><body>Text2</body></html>".getBytes("UTF-8")), byte[].class);
        textValue.searchIndex(false);
        metadata = new Metadata();
        metadata.add(VisalloProperties.MIME_TYPE.getPropertyName(), "text/html", getVisibilityTranslator().getDefaultVisibility());
        v.setProperty("http://visallo.org/test#raw2", textValue, metadata, visibility);

        v.save(getGraphAuthorizations());

        Vertex vertex = getGraph().getVertex("v1", getGraphAuthorizations());
        run(gpw, getWorkerPrepareData(), vertex);

        Property text1Property = vertex.getProperty("http://visallo.org/test#text1");
        assertTrue(text1Property != null);
        assertThat(((StreamingPropertyValue) text1Property.getValue()).readToString(), equalTo("Text1"));
        assertThat(VisalloProperties.TEXT_DESCRIPTION_METADATA.getMetadataValue(text1Property.getMetadata()), equalTo("Text 1"));

        Property text2Property = vertex.getProperty("http://visallo.org/test#text2");
        assertTrue(text2Property != null);
        assertThat(((StreamingPropertyValue) text2Property.getValue()).readToString(), equalTo("Text2"));
        assertThat(VisalloProperties.TEXT_DESCRIPTION_METADATA.getMetadataValue(text2Property.getMetadata()), equalTo(null));
    }

    //todo : add test with image metadata
}
