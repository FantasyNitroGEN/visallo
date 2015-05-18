package org.visallo.it;

import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.MediaVisalloProperties;
import org.visallo.tesseract.TesseractGraphPropertyWorker;
import org.visallo.web.clientapi.VisalloApi;
import org.visallo.web.clientapi.VertexApiExt;
import org.visallo.web.clientapi.codegen.ApiException;
import org.visallo.web.clientapi.model.ClientApiArtifactImportResponse;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiProperty;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class UploadVideoFileIntegrationTest extends TestBase {
    private String artifactVertexId;

    @Test
    public void testUploadFile() throws IOException, ApiException {
        importVideoAndPublishAsUser1();
        assertRawRoute();
        assertRawRoutePlayback();
        assertPosterFrameRoute();
        assertVideoPreviewRoute();
        resolveTermsAsUser1();
    }

    private void importVideoAndPublishAsUser1() throws ApiException, IOException {
        LOGGER.info("importVideoAndPublishAsUser1");
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);
        addUserAuths(visalloApi, USERNAME_TEST_USER_1, "auth1");

        InputStream videoResourceStream = UploadVideoFileIntegrationTest.class.getResourceAsStream("/org/visallo/it/shortVideo.mp4");
        InputStream videoTranscriptResourceStream = UploadVideoFileIntegrationTest.class.getResourceAsStream("/org/visallo/it/shortVideo.mp4.srt");
        ClientApiArtifactImportResponse artifact = visalloApi.getVertexApi().importFiles(
                new VertexApiExt.FileForImport("auth1", "shortVideo.mp4", videoResourceStream),
                new VertexApiExt.FileForImport("auth1", "shortVideo.mp4.srt", videoTranscriptResourceStream));
        assertEquals(1, artifact.getVertexIds().size());
        artifactVertexId = artifact.getVertexIds().get(0);
        assertNotNull(artifactVertexId);

        visalloTestCluster.processGraphPropertyQueue();

        boolean foundTesseractVideoTranscript = false;
        ClientApiElement vertex = visalloApi.getVertexApi().getByVertexId(artifactVertexId);
        for (ClientApiProperty prop : vertex.getProperties()) {
            LOGGER.info(prop.toString());
            if (VisalloProperties.TEXT.getPropertyName().equals(prop.getName()) || MediaVisalloProperties.VIDEO_TRANSCRIPT.getPropertyName().equals(prop.getName())) {
                String highlightedText = visalloApi.getVertexApi().getHighlightedText(artifactVertexId, prop.getKey());
                LOGGER.info("highlightedText: %s: %s: %s", prop.getName(), prop.getKey(), highlightedText);
                if (prop.getKey().equals(TesseractGraphPropertyWorker.TEXT_PROPERTY_KEY)) {
                    foundTesseractVideoTranscript = true;
                    assertTrue("invalid highlighted text for tesseract", highlightedText.contains("Test") && highlightedText.contains("12000"));
                }
            }
        }
        assertTrue("foundTesseractVideoTranscript", foundTesseractVideoTranscript);

        assertPublishAll(visalloApi, 25);

        visalloApi.logout();
    }

    private void assertRawRoute() throws ApiException, IOException {
        LOGGER.info("assertRawRoute");
        byte[] expected = IOUtils.toByteArray(UploadVideoFileIntegrationTest.class.getResourceAsStream("/org/visallo/it/shortVideo.mp4"));

        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        byte[] found = IOUtils.toByteArray(visalloApi.getVertexApi().getRaw(artifactVertexId));
        assertArrayEquals(expected, found);

        visalloApi.logout();
    }

    private void assertRawRoutePlayback() throws ApiException, IOException {
        LOGGER.info("assertRawRoutePlayback");
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        byte[] found = IOUtils.toByteArray(visalloApi.getVertexApi().getRawForPlayback(artifactVertexId, MediaVisalloProperties.MIME_TYPE_VIDEO_MP4));
        assertTrue(found.length > 0);

        visalloApi.logout();
    }

    private void assertPosterFrameRoute() throws ApiException, IOException {
        LOGGER.info("assertPosterFrameRoute");
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        InputStream in = visalloApi.getVertexApi().getPosterFrame(artifactVertexId, 100);
        BufferedImage img = ImageIO.read(in);
        assertEquals(100, img.getWidth());
        assertEquals(66, img.getHeight());

        visalloApi.logout();
    }

    private void assertVideoPreviewRoute() throws IOException, ApiException {
        LOGGER.info("assertVideoPreviewRoute");
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        InputStream in = visalloApi.getVertexApi().getVideoPreview(artifactVertexId, 100);
        BufferedImage img = ImageIO.read(in);
        assertEquals(2000, img.getWidth());
        assertEquals(66, img.getHeight());

        visalloApi.logout();
    }

    private void resolveTermsAsUser1() throws ApiException {
        LOGGER.info("resolveTermsAsUser1");
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        String propertyKey = "SubRipTranscriptGraphPropertyWorker";
        int videoFrameIndex = 0;
        int mentionStart = "".length();
        int mentionEnd = mentionStart + "Salam".length();
        visalloApi.getVertexApi().resolveVideoTranscriptTerm(artifactVertexId, propertyKey, videoFrameIndex, mentionStart, mentionEnd, "Salam", TestOntology.CONCEPT_PERSON, "auth1");

        videoFrameIndex = 2;
        mentionStart = "appalling brutality what we know is that\nthree ".length();
        mentionEnd = mentionStart + "British".length();
        visalloApi.getVertexApi().resolveVideoTranscriptTerm(artifactVertexId, propertyKey, videoFrameIndex, mentionStart, mentionEnd, "Great Britain", TestOntology.CONCEPT_PERSON, "auth1");

        visalloTestCluster.processGraphPropertyQueue();

        String highlightedText = visalloApi.getVertexApi().getHighlightedText(artifactVertexId, propertyKey);
        LOGGER.info(highlightedText);
        assertTrue("missing highlighting for Salam", highlightedText.contains(">Salam<"));
        assertTrue("missing highlighting for British", highlightedText.contains("three <span") && highlightedText.contains(">British<"));

        visalloApi.logout();
    }
}
