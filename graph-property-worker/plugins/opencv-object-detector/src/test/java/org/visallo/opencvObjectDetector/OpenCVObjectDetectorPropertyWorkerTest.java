package org.visallo.opencvObjectDetector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.ingest.ArtifactDetectedObject;
import org.visallo.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import org.visallo.core.model.file.ClassPathFileSystemRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.test.GraphPropertyWorkerTestBase;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpenCVObjectDetectorPropertyWorkerTest extends GraphPropertyWorkerTestBase {
    private static final String TEST_IMAGE = "testFsRoot/cnn.jpg";
    public static final String TEST_FS_ROOT = "/testFsRoot";

    @Mock
    private ArtifactThumbnailRepository artifactThumbnailRepository;

    @Mock
    private OntologyRepository ontologyRepository;

    @Before
    public void setUp() throws Exception {
        System.out.println(System.getProperty("java.library.path"));
    }

    @Test
    public void testObjectDetection() throws Exception {
        when(ontologyRepository.getRequiredConceptIRIByIntent(eq("face"))).thenReturn("http://test.visallo.org/#face");

        OpenCVObjectDetectorPropertyWorker objectDetector = new OpenCVObjectDetectorPropertyWorker(
                artifactThumbnailRepository,
                new ClassPathFileSystemRepository(TEST_FS_ROOT)
        );
        objectDetector.setConfiguration(getConfiguration());
        objectDetector.setOntologyRepository(ontologyRepository);
        objectDetector.loadNativeLibrary();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        BufferedImage bImage = ImageIO.read(cl.getResourceAsStream(TEST_IMAGE));
        objectDetector.prepare(getWorkerPrepareData());

        List<ArtifactDetectedObject> detectedObjectList = objectDetector.detectObjects(bImage);
        assertTrue("Incorrect number of objects found", detectedObjectList.size() == 1);

        ArtifactDetectedObject detectedObject = detectedObjectList.get(0);
        assertEquals("http://test.visallo.org/#face", detectedObject.getConcept());
        assertEquals(0.423828125, detectedObject.getX1(), 0.01);
        assertEquals(0.1828125, detectedObject.getY1(), 0.01);
        assertEquals(0.6220703125, detectedObject.getX2(), 0.01);
        assertEquals(0.5, detectedObject.getY2(), 0.01);
    }

    @Override
    protected Map getConfigurationMap() {
        Map result = super.getConfigurationMap();
        result.put("org.visallo.opencvObjectDetector.OpenCVObjectDetectorPropertyWorker.classifier.face.path", "/haarcascade_frontalface_alt.xml");
        return result;
    }
}
