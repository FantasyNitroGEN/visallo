package org.visallo.imageMetadataExtractor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Metadata;
import org.vertexium.Vertex;
import org.vertexium.VertexBuilder;
import org.vertexium.Visibility;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.util.VisalloDateTime;
import org.visallo.test.GraphPropertyWorkerTestBase;

import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ImageMetadataGraphPropertyWorkerTest extends GraphPropertyWorkerTestBase {
    public static final String MEDIA_IMAGE_HEADING = "http://visallo.org/test#media.imageHeading";
    public static final String GEO_LOCATION = "http://visallo.org/test#geoLocation";
    public static final String MEDIA_DATE_TAKEN = "http://visallo.org/test#media.dateTaken";
    public static final String MEDIA_DEVICE_MAKE = "http://visallo.org/test#media.deviceMake";
    public static final String MEDIA_DEVICE_MODEL = "http://visallo.org/test#media.deviceModel";
    public static final String MEDIA_WIDTH = "http://visallo.org/test#media.width";
    public static final String MEDIA_HEIGHT = "http://visallo.org/test#media.height";
    public static final String MEDIA_METADATA = "http://visallo.org/test#media.metadata";
    public static final String MEDIA_FILE_SIZE = "http://visallo.org/test#media.fileSize";
    private ImageMetadataGraphPropertyWorker gpw;
    private Visibility visibility = new Visibility("");

    @Mock
    private OntologyRepository ontologyRepository;

    @Before
    public void setUp() {
        when(ontologyRepository.getRequiredPropertyIRIByIntent(eq("media.imageHeading"))).thenReturn(MEDIA_IMAGE_HEADING);
        when(ontologyRepository.getRequiredPropertyIRIByIntent(eq("geoLocation"))).thenReturn(GEO_LOCATION);
        when(ontologyRepository.getRequiredPropertyIRIByIntent(eq("media.dateTaken"))).thenReturn(MEDIA_DATE_TAKEN);
        when(ontologyRepository.getRequiredPropertyIRIByIntent(eq("media.deviceMake"))).thenReturn(MEDIA_DEVICE_MAKE);
        when(ontologyRepository.getRequiredPropertyIRIByIntent(eq("media.deviceModel"))).thenReturn(MEDIA_DEVICE_MODEL);
        when(ontologyRepository.getRequiredPropertyIRIByIntent(eq("media.width"))).thenReturn(MEDIA_WIDTH);
        when(ontologyRepository.getRequiredPropertyIRIByIntent(eq("media.height"))).thenReturn(MEDIA_HEIGHT);
        when(ontologyRepository.getRequiredPropertyIRIByIntent(eq("media.metadata"))).thenReturn(MEDIA_METADATA);
        when(ontologyRepository.getRequiredPropertyIRIByIntent(eq("media.fileSize"))).thenReturn(MEDIA_FILE_SIZE);

        gpw = new ImageMetadataGraphPropertyWorker();
        gpw.setOntologyRepository(ontologyRepository);
    }

    @Test
    public void testRun() throws IOException {
        VertexBuilder m = getGraph().prepareVertex("v1", visibility);
        Metadata metadata = new Metadata();
        VisalloProperties.MIME_TYPE_METADATA.setMetadata(metadata, "image/jpeg", visibility);
        StreamingPropertyValue spv = getStreamingPropertyValueFromResource("img_1771.jpg");
        VisalloProperties.RAW.setProperty(m, spv, metadata, visibility);
        m.save(getGraphAuthorizations());
        getGraph().flush();

        Vertex v1 = getGraph().getVertex("v1", getGraphAuthorizations(""));
        run(gpw, getWorkerPrepareData(), v1, VisalloProperties.RAW.getProperty(v1), VisalloProperties.RAW.getPropertyValue(v1).getInputStream());

        v1 = getGraph().getVertex("v1", getGraphAuthorizations(""));
        assertEquals(null, v1.getPropertyValue(ImageMetadataGraphPropertyWorker.MULTI_VALUE_KEY, MEDIA_IMAGE_HEADING));
        assertEquals(null, v1.getPropertyValue(ImageMetadataGraphPropertyWorker.MULTI_VALUE_KEY, GEO_LOCATION));
        Date expectedTime = VisalloDateTime.parse("2003-12-14T07:01:44", null).getJavaDate(); // timezone will be local
        Date actualTime = (Date) v1.getPropertyValue(ImageMetadataGraphPropertyWorker.MULTI_VALUE_KEY, MEDIA_DATE_TAKEN);
        assertEquals(expectedTime, actualTime);
        assertEquals("Canon", v1.getPropertyValue(ImageMetadataGraphPropertyWorker.MULTI_VALUE_KEY, MEDIA_DEVICE_MAKE));
        assertEquals("Canon PowerShot S40", v1.getPropertyValue(ImageMetadataGraphPropertyWorker.MULTI_VALUE_KEY, MEDIA_DEVICE_MODEL));
        assertEquals(480, v1.getPropertyValue(ImageMetadataGraphPropertyWorker.MULTI_VALUE_KEY, MEDIA_WIDTH));
        assertEquals(360, v1.getPropertyValue(ImageMetadataGraphPropertyWorker.MULTI_VALUE_KEY, MEDIA_HEIGHT));
        assertEquals(32764, v1.getPropertyValue(ImageMetadataGraphPropertyWorker.MULTI_VALUE_KEY, MEDIA_FILE_SIZE));
        String metadataData = v1.getPropertyValue(ImageMetadataGraphPropertyWorker.MULTI_VALUE_KEY, MEDIA_METADATA).toString();
        assertTrue(metadataData.contains("Top, left side (Horizontal / normal)"));
    }

    private StreamingPropertyValue getStreamingPropertyValueFromResource(String resourceName) {
        StreamingPropertyValue spv = new StreamingPropertyValue(this.getClass().getResourceAsStream(resourceName), byte[].class);
        spv.store(true);
        spv.searchIndex(false);
        return spv;
    }
}
