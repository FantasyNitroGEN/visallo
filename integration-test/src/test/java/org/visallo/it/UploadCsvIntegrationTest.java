package org.visallo.it;

import org.junit.Test;
import org.visallo.csv.CsvOntology;
import org.visallo.web.clientapi.VisalloApi;
import org.visallo.web.clientapi.codegen.ApiException;
import org.visallo.web.clientapi.model.ClientApiArtifactImportResponse;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiElementSearchResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static junit.framework.TestCase.assertEquals;

public class UploadCsvIntegrationTest extends TestBase {
    private static final String FILE_CONTENTS = getResourceString("sample.csv");
    private static final String MAPPING_JSON = getResourceString("sample.csv.mapping.json");
    private String artifactVertexId;

    @Test
    public void testUploadCsv() throws IOException, ApiException {
        uploadAndProcessCsv();
        assertUser2CanSeeCsvVertices();
    }

    public void uploadAndProcessCsv() throws ApiException, IOException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);
        addUserAuths(visalloApi, USERNAME_TEST_USER_1, "auth1");

        ClientApiArtifactImportResponse artifact = visalloApi.getVertexApi().importFile("auth1", "sample.csv", new ByteArrayInputStream(FILE_CONTENTS.getBytes()));
        artifactVertexId = artifact.getVertexIds().get(0);

        visalloApi.getVertexApi().setProperty(artifactVertexId, "", CsvOntology.MAPPING_JSON.getPropertyName(), MAPPING_JSON, "", "");

        visalloTestCluster.processGraphPropertyQueue();

        assertPublishAll(visalloApi, 46);

        ClientApiElementSearchResponse searchResults = visalloApi.getVertexApi().vertexSearch("*");
        LOGGER.info("searchResults (user1): %s", searchResults);
        assertEquals(8, searchResults.getElements().size());
        for (ClientApiElement e : searchResults.getElements()) {
            assertEquals("auth1", e.getVisibilitySource());
        }

        visalloApi.logout();
    }

    private void assertUser2CanSeeCsvVertices() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_2);
        addUserAuths(visalloApi, USERNAME_TEST_USER_2, "auth1");

        ClientApiElementSearchResponse searchResults = visalloApi.getVertexApi().vertexSearch("*");
        LOGGER.info("searchResults (user2): %s", searchResults);
        assertEquals(8, searchResults.getElements().size());

        visalloApi.logout();
    }
}
