package org.visallo.it;

import com.google.common.collect.ImmutableList;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.VisalloApi;
import org.visallo.web.clientapi.codegen.ApiException;
import org.junit.Test;
import org.visallo.web.clientapi.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class UploadRdfIntegrationTest extends TestBase {
    private static final String FILE_CONTENTS = getResourceString("sample.rdf");
    private String artifactVertexId;
    private String joeFernerVertexId;
    private String daveSingleyVertexId;
    private String v5AnalyticsVertexId;

    @Test
    public void testUploadRdf() throws IOException, ApiException {
        uploadAndProcessRdf();
        assertUser2CanSeeRdfVertices();
        assertGetEdgesWithVisibilities();
    }

    public void uploadAndProcessRdf() throws ApiException, IOException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);
        addUserAuths(visalloApi, USERNAME_TEST_USER_1, "auth1");
        addUserAuths(visalloApi, USERNAME_TEST_USER_1, "auth2");

        ClientApiArtifactImportResponse artifact = visalloApi.getVertexApi().importFile("auth1", "sample.rdf", new ByteArrayInputStream(FILE_CONTENTS.getBytes()));
        artifactVertexId = artifact.getVertexIds().get(0);

        visalloTestCluster.processGraphPropertyQueue();

        assertPublishAll(visalloApi, 25);

        ClientApiVertexSearchResponse searchResults = visalloApi.getVertexApi().vertexSearch("*");
        LOGGER.info("searchResults (user1): %s", searchResults);
        assertEquals(4, searchResults.getVertices().size());
        for (ClientApiVertex v : searchResults.getVertices()) {
            assertEquals("auth1", v.getVisibilitySource());

            if (v.getId().equals("PERSON_Joe_Ferner")) {
                joeFernerVertexId = v.getId();
            }
            if (v.getId().equals("PERSON_Dave_Singley")) {
                daveSingleyVertexId = v.getId();
            }
            if (v.getId().equals("COMPANY_v5analytics")) {
                v5AnalyticsVertexId = v.getId();
            }
        }
        assertNotNull(joeFernerVertexId, "Could not find joe ferner");
        assertNotNull(daveSingleyVertexId, "Could not find dave singley");

        visalloApi.logout();
    }

    private void assertUser2CanSeeRdfVertices() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_2);
        addUserAuths(visalloApi, USERNAME_TEST_USER_2, "auth1");

        assertSearch(visalloApi);
        assertGetEdges(visalloApi);

        // TODO/BUG The following is commented out until long-running processes are supported in integration tests.
        //          Leaving this in causes the test to hang indefinitely because the long-running process queue is
        //          not being read.
        // assertFindPath(visalloApi);

        assertFindRelated(visalloApi);
        assertFindMultiple(visalloApi);
        assertWorkspace(visalloApi);

        visalloApi.logout();
    }

    private void assertFindMultiple(VisalloApi visalloApi) throws ApiException {
        List<String> graphVertexIds = new ArrayList<>();
        graphVertexIds.add(artifactVertexId);
        graphVertexIds.add(joeFernerVertexId);
        graphVertexIds.add(daveSingleyVertexId);
        graphVertexIds.add(v5AnalyticsVertexId);
        ClientApiVertexMultipleResponse vertices = visalloApi.getVertexApi().findMultiple(graphVertexIds, true);
        LOGGER.info("vertices: %s", vertices.toString());
        assertEquals(4, vertices.getVertices().size());
        assertFalse("isRequiredFallback", vertices.isRequiredFallback());
        boolean foundV5Analytics = false;
        boolean foundArtifact = false;
        boolean foundDaveSingley = false;
        boolean foundJoeFerner = false;
        for (ClientApiVertex v : vertices.getVertices()) {
            if (v.getId().equals(v5AnalyticsVertexId)) {
                foundV5Analytics = true;
            }
            if (v.getId().equals(artifactVertexId)) {
                foundArtifact = true;
            }
            if (v.getId().equals(joeFernerVertexId)) {
                foundDaveSingley = true;
            }
            if (v.getId().equals(daveSingleyVertexId)) {
                foundJoeFerner = true;
            }
        }
        assertTrue("could not find V5 Analytics in multiple", foundV5Analytics);
        assertTrue("could not find Artifact in multiple", foundArtifact);
        assertTrue("could not find DaveSingley in multiple", foundDaveSingley);
        assertTrue("could not find JoeFerner in multiple", foundJoeFerner);
    }

    private void assertFindRelated(VisalloApi visalloApi) throws ApiException {
        ClientApiVertexFindRelatedResponse related = visalloApi.getVertexApi().findRelated(ImmutableList.of(joeFernerVertexId));
        assertEquals(2, related.getCount());
        assertEquals(2, related.getVertices().size());

        boolean foundV5Analytics = false;
        boolean foundRdfDocument = false;
        for (ClientApiVertex v : related.getVertices()) {
            if (v.getId().equals(v5AnalyticsVertexId)) {
                foundV5Analytics = true;
            }
            if (v.getId().equals(artifactVertexId)) {
                foundRdfDocument = true;
            }
        }
        assertTrue("could not find V5 Analytics in related", foundV5Analytics);
        assertTrue("could not find rdf in related", foundRdfDocument);
    }

    private void assertSearch(VisalloApi visalloApi) throws ApiException {
        ClientApiVertexSearchResponse searchResults = visalloApi.getVertexApi().vertexSearch("*");
        LOGGER.info("searchResults (user2): %s", searchResults);
        assertEquals(4, searchResults.getVertices().size());
    }

    private void assertGetEdges(VisalloApi visalloApi) throws ApiException {
        ClientApiVertexEdges artifactEdges = visalloApi.getVertexApi().getEdges(artifactVertexId, null, null, null);
        assertEquals(3, artifactEdges.getTotalReferences());
        assertEquals(3, artifactEdges.getRelationships().size());

        for (ClientApiVertexEdges.Edge e : artifactEdges.getRelationships()) {
            String edgeId = e.getRelationship().getId();
            ClientApiEdgeWithVertexData edge = visalloApi.getEdgeApi().getByEdgeId(edgeId);
            LOGGER.info("edge: %s", edge.toString());
        }
    }

    private void assertFindPath(VisalloApi visalloApi) throws ApiException {
        ClientApiLongRunningProcessSubmitResponse longRunningProcessResponse = visalloApi.getVertexApi().findPath(joeFernerVertexId, daveSingleyVertexId, 2);
        ClientApiLongRunningProcess longRunningProcess = visalloApi.getLongRunningProcessApi().waitById(longRunningProcessResponse.getId());
        String resultsString = longRunningProcess.getResultsString();
        assertNotNull("resultsString was null", resultsString);
        ClientApiVertexFindPathResponse paths = ClientApiConverter.toClientApi(resultsString, ClientApiVertexFindPathResponse.class);

        LOGGER.info("paths: %s", paths.toString());
        assertEquals(2, paths.getPaths().size());
        boolean foundV5Analytics = false;
        boolean foundRdfDocument = false;
        for (List<ClientApiVertex> path : paths.getPaths()) {
            assertEquals(3, path.size());
            if (path.get(1).getId().equals(v5AnalyticsVertexId)) {
                foundV5Analytics = true;
            }
            if (path.get(1).getId().equals(artifactVertexId)) {
                foundRdfDocument = true;
            }
        }
        assertTrue("could not find V5 Analytics in path", foundV5Analytics);
        assertTrue("could not find rdf in path", foundRdfDocument);
    }

    private void assertWorkspace(VisalloApi visalloApi) throws ApiException {
        addAllVerticesExceptArtifactToWorkspace(visalloApi);
        assertWorkspaceVertices(visalloApi);
        assertWorkspaceEdges(visalloApi);
    }

    private void addAllVerticesExceptArtifactToWorkspace(VisalloApi visalloApi) throws ApiException {
        ClientApiVertexSearchResponse vertices = visalloApi.getVertexApi().vertexSearch("*");
        ClientApiWorkspaceUpdateData workspaceUpdateData = new ClientApiWorkspaceUpdateData();
        for (ClientApiVertex v : vertices.getVertices()) {
            if (v.getId().equals(artifactVertexId)) {
                continue;
            }
            ClientApiWorkspaceUpdateData.EntityUpdate entityUpdate = new ClientApiWorkspaceUpdateData.EntityUpdate();
            entityUpdate.setVertexId(v.getId());
            entityUpdate.setGraphPosition(new GraphPosition(10, 10));
            workspaceUpdateData.getEntityUpdates().add(entityUpdate);
        }
        visalloApi.getWorkspaceApi().update(workspaceUpdateData);
    }

    private void assertWorkspaceVertices(VisalloApi visalloApi) throws ApiException {
        ClientApiWorkspaceVertices vertices = visalloApi.getWorkspaceApi().getVertices();
        LOGGER.info("workspace vertices: %s", vertices.toString());
        assertEquals(3, vertices.getVertices().size());
    }

    private void assertWorkspaceEdges(VisalloApi visalloApi) throws ApiException {
        List<String> additionalIds = new ArrayList<>();
        additionalIds.add(artifactVertexId);
        ClientApiWorkspaceEdges edges = visalloApi.getWorkspaceApi().getEdges(additionalIds);
        LOGGER.info("workspace edges: %s", edges.toString());
        assertEquals(5, edges.getEdges().size());
    }

    private void assertGetEdgesWithVisibilities() throws ApiException {
        int existingEdgeCount = 0;
        long existingEdgeTotalCount = 0;
        String v5AnalyticsVertexId = null;

        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        ClientApiVertexSearchResponse vertices = visalloApi.getVertexApi().vertexSearch("*");
        boolean foundAndChangedVertexVisibility = false;
        for (ClientApiVertex v : vertices.getVertices()) {
            if (v.getId().contains("PERSON_Joe_Ferner")) {
                visalloApi.getVertexApi().setVisibility(v.getId(), "auth2");
                foundAndChangedVertexVisibility = true;
            } else if (v.getId().contains("COMPANY_v5analytics")) {
                v5AnalyticsVertexId = v.getId();
                ClientApiVertexEdges existingEdges = visalloApi.getVertexApi().getEdges(v5AnalyticsVertexId, null, 0, 100);
                existingEdgeCount = existingEdges.getRelationships().size();
                existingEdgeTotalCount = existingEdges.getTotalReferences();
            }
        }
        assertTrue("could not find or change vertex visibility", foundAndChangedVertexVisibility);
        assertNotNull("v5AnalyticsVertexId was null", v5AnalyticsVertexId);
        assertEquals(3, existingEdgeCount);
        assertEquals(3, existingEdgeTotalCount);

        visalloApi.logout();

        visalloApi = login(USERNAME_TEST_USER_2);

        ClientApiVertexEdges existingEdges = visalloApi.getVertexApi().getEdges(v5AnalyticsVertexId, null, 0, 100);
        assertEquals(2, existingEdges.getRelationships().size());
        assertEquals(2, existingEdges.getTotalReferences());

        existingEdges = visalloApi.getVertexApi().getEdges(v5AnalyticsVertexId, null, 0, 1);
        assertEquals(1, existingEdges.getRelationships().size());
        assertEquals(2, existingEdges.getTotalReferences());

        visalloApi.logout();
    }
}
