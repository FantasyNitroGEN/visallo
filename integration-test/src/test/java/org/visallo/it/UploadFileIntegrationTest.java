package org.visallo.it;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.visallo.core.ingest.FileImport;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker;
import org.visallo.web.clientapi.VisalloApi;
import org.visallo.web.clientapi.codegen.ApiException;
import org.visallo.web.clientapi.model.*;
import org.visallo.web.clientapi.util.ObjectMapperFactory;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.vertexium.type.GeoPoint;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class UploadFileIntegrationTest extends TestBase {
    public static final String FILE_CONTENTS = "Joe Ferner knows David Singley.";
    private String user2Id;
    private String workspaceId;
    private String artifactVertexId;

    @Test
    public void testUploadFile() throws IOException, ApiException {
        testOntology();
        importArtifactAsUser1();
        assertUser1CanSeeInSearch();
        assertUser2DoesNotHaveAccessToUser1sWorkspace();
        grantUser2AccessToWorkspace();
        assertUser2HasAccessToWorkspace();
        assertUser3DoesNotHaveAccessToWorkspace();
        publishArtifact();
        assertUser3StillHasNoAccessToArtifactBecauseAuth1Visibility();
        assertUser3HasAccessWithAuth1Visibility();
        assertRawRoute();
        alterVisibilityOfArtifactToAuth2();
        assertUser2DoesNotHaveAccessToAuth2();
        testGeoSearch();
        testSetTitleAndCheckConfidence();
    }

    private void testOntology() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        ClientApiOntology ontology = visalloApi.getOntologyApi().get();

        boolean foundPersonConcept = false;
        for (ClientApiOntology.Concept concept : ontology.getConcepts()) {
            if (concept.getId().equals("http://visallo.org/test#person")) {
                foundPersonConcept = true;
                assertEquals("invalid title formula", "prop('http://visallo.org/test#firstName') + ' ' + prop('http://visallo.org/test#lastName')", concept.getTitleFormula());
                assertEquals("invalid sub-title formula", "prop('http://visallo.org/test#firstName') || ''", concept.getSubtitleFormula());
                assertEquals("invalid time formula", "prop('http://visallo.org/test#birthDate') || ''", concept.getTimeFormula());
            }
        }
        assertTrue("could not find http://visallo.org/test#person", foundPersonConcept);

        visalloApi.logout();
    }

    public void importArtifactAsUser1() throws ApiException, IOException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);
        addUserAuths(visalloApi, USERNAME_TEST_USER_1, "auth1");
        addUserAuths(visalloApi, USERNAME_TEST_USER_1, "auth2");
        workspaceId = visalloApi.getCurrentWorkspaceId();

        ClientApiArtifactImportResponse artifact = visalloApi.getVertexApi().importFile("auth1", "test.txt", new ByteArrayInputStream(FILE_CONTENTS.getBytes()));
        assertEquals(1, artifact.getVertexIds().size());
        artifactVertexId = artifact.getVertexIds().get(0);
        assertNotNull(artifactVertexId);

        visalloTestCluster.processGraphPropertyQueue();

        assertArtifactCorrect(visalloApi, true, "auth1");

        visalloApi.logout();
    }

    private void assertUser1CanSeeInSearch() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        ClientApiVertexSearchResponse searchResults = visalloApi.getVertexApi().vertexSearch("*");
        LOGGER.debug("searchResults: %s", searchResults.toString());
        assertEquals(1, searchResults.getVertices().size());
        ClientApiVertex searchResult = searchResults.getVertices().get(0);
        assertEquals(artifactVertexId, searchResult.getId());

        visalloApi.logout();
    }

    public void assertUser2DoesNotHaveAccessToUser1sWorkspace() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_2);
        addUserAuths(visalloApi, USERNAME_TEST_USER_2, "auth1");
        user2Id = visalloApi.getCurrentUserId();

        visalloApi.setWorkspaceId(workspaceId);
        try {
            visalloApi.getVertexApi().getByVertexId(artifactVertexId);
            assertTrue("should have failed", false);
        } catch (ApiException ex) {
            // expected
        }

        visalloApi.logout();
    }

    public void grantUser2AccessToWorkspace() throws ApiException {
        VisalloApi visalloApi;
        visalloApi = login(USERNAME_TEST_USER_1);
        visalloApi.setWorkspaceId(workspaceId);
        visalloApi.getWorkspaceApi().setUserAccess(user2Id, WorkspaceAccess.READ);
        visalloApi.logout();
    }

    public void assertUser2HasAccessToWorkspace() throws ApiException {
        VisalloApi visalloApi;
        visalloApi = login(USERNAME_TEST_USER_2);
        visalloApi.setWorkspaceId(workspaceId);
        ClientApiElement artifactVertex = visalloApi.getVertexApi().getByVertexId(artifactVertexId);
        assertNotNull(artifactVertex);
        visalloApi.logout();
    }

    public void assertUser3DoesNotHaveAccessToWorkspace() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_3);
        visalloApi.setWorkspaceId(workspaceId);
        try {
            visalloApi.getVertexApi().getByVertexId(artifactVertexId);
            assertTrue("should have failed", false);
        } catch (ApiException ex) {
            // expected
        }
        visalloApi.logout();
    }

    private void publishArtifact() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);
        assertPublishAll(visalloApi, 11);
        visalloApi.logout();
    }

    private void assertUser3StillHasNoAccessToArtifactBecauseAuth1Visibility() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_3);
        ClientApiElement vertex = visalloApi.getVertexApi().getByVertexId(artifactVertexId);
        assertNull("should have failed", vertex);
        visalloApi.logout();
    }

    private void assertUser3HasAccessWithAuth1Visibility() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_3);
        addUserAuths(visalloApi, USERNAME_TEST_USER_3, "auth1");
        assertArtifactCorrect(visalloApi, false, "auth1");
        visalloApi.logout();
    }

    public void assertArtifactCorrect(VisalloApi visalloApi, boolean hasWorkspaceIdInVisibilityJson, String expectedVisibilitySource) throws ApiException {
        ClientApiElement artifactVertex = visalloApi.getVertexApi().getByVertexId(artifactVertexId);
        assertNotNull("could not get vertex: " + artifactVertexId, artifactVertex);
        assertEquals(expectedVisibilitySource, artifactVertex.getVisibilitySource());
        assertEquals(artifactVertexId, artifactVertex.getId());
        for (ClientApiProperty property : artifactVertex.getProperties()) {
            LOGGER.info("property: %s", property.toString());
        }
        assertEquals(11, artifactVertex.getProperties().size());
        assertHasProperty(artifactVertex.getProperties(), TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY, VisalloProperties.MODIFIED_DATE.getPropertyName());
        assertHasProperty(artifactVertex.getProperties(), "", VisalloProperties.MIME_TYPE.getPropertyName(), "text/plain");
        assertHasProperty(artifactVertex.getProperties(), TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY, VisalloProperties.TEXT.getPropertyName());
        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.setSource(expectedVisibilitySource);
        if (hasWorkspaceIdInVisibilityJson) {
            visibilityJson.addWorkspace(workspaceId);
        }
        assertHasProperty(artifactVertex.getProperties(), "", VisalloProperties.VISIBILITY_JSON.getPropertyName(), visibilityJson);
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, VisalloProperties.CONTENT_HASH.getPropertyName(), "urn\u001Fsha256\u001F28fca952b9eb45d43663af8e3099da0572c8232243289b5d8a03eb5ea2cb066a");
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, VisalloProperties.MODIFIED_DATE.getPropertyName());
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, VisalloProperties.FILE_NAME.getPropertyName(), "test.txt");
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, VisalloProperties.RAW.getPropertyName());
        assertHasProperty(artifactVertex.getProperties(), FileImport.MULTI_VALUE_KEY, VisalloProperties.TITLE.getPropertyName(), "test.txt");
        assertHasProperty(artifactVertex.getProperties(), "", VisalloProperties.CONCEPT_TYPE.getPropertyName(), "http://visallo.org/test#document");

        String highlightedText = visalloApi.getVertexApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        assertNotNull(highlightedText);
        LOGGER.info("highlightedText: %s", highlightedText);
        assertTrue("highlightedText did not contain string: " + highlightedText, highlightedText.contains("class=\"entity\""));
        assertTrue("highlightedText did not contain string: " + highlightedText, highlightedText.contains(TestOntology.CONCEPT_PERSON));
    }

    private void assertRawRoute() throws ApiException, IOException {
        byte[] expected = FILE_CONTENTS.getBytes();

        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        byte[] found = IOUtils.toByteArray(visalloApi.getVertexApi().getRaw(artifactVertexId));
        assertArrayEquals(expected, found);

        visalloApi.logout();
    }

    private void alterVisibilityOfArtifactToAuth2() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        visalloApi.getVertexApi().setVisibility(artifactVertexId, "auth2");
        assertArtifactCorrect(visalloApi, false, "auth2");

        visalloApi.logout();
    }

    private void assertUser2DoesNotHaveAccessToAuth2() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_2);

        visalloApi.getVertexApi().getByVertexId(artifactVertexId);

        visalloApi.logout();
    }

    private void testGeoSearch() throws ApiException, JsonProcessingException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        String geoPoint = ObjectMapperFactory.getInstance().writeValueAsString(new GeoPoint(38.8951, -77.0367));
        visalloApi.getVertexApi().setProperty(artifactVertexId, "", TestOntology.PROPERTY_GEO_LOCATION.getPropertyName(), geoPoint, "", "justification", null, null);

        ClientApiVertexSearchResponse geoSearchResults = visalloApi.getVertexApi().vertexGeoSearch(38.8951, -77.0367, 1000.0);
        assertEquals(1, geoSearchResults.getVertices().size());

        visalloApi.logout();
    }

    private void testSetTitleAndCheckConfidence() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        ClientApiWorkspace newWorkspace = visalloApi.getWorkspaceApi().create();
        visalloApi.setWorkspaceId(newWorkspace.getWorkspaceId());

        visalloApi.getVertexApi().setProperty(artifactVertexId, "", VisalloProperties.TITLE.getPropertyName(), "New Title", "", "new title");

        ClientApiElement artifactVertex = visalloApi.getVertexApi().getByVertexId(artifactVertexId);
        boolean foundNewTitle = false;
        for (ClientApiProperty prop : artifactVertex.getProperties()) {
            if (prop.getKey().equals("") && prop.getName().equals(VisalloProperties.TITLE.getPropertyName())) {
                foundNewTitle = true;
                LOGGER.info("new title prop: %s", prop.toString());
                assertNotNull("could not find confidence", VisalloProperties.CONFIDENCE_METADATA.getMetadataValue(prop.getMetadata()));
                assertEquals(GraphRepository.SET_PROPERTY_CONFIDENCE, VisalloProperties.CONFIDENCE_METADATA.getMetadataValue(prop.getMetadata()), 0.01);
            }
        }
        assertTrue("Could not find new title", foundNewTitle);

        visalloApi.logout();
        visalloApi.setWorkspaceId(workspaceId);
    }
}
