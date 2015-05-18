package org.visallo.it;

import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker;
import org.visallo.web.clientapi.VisalloApi;
import org.visallo.web.clientapi.codegen.ApiException;
import org.junit.Test;
import org.visallo.web.clientapi.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class ResolveTermIntegrationTest extends TestBase {
    private String artifactVertexId;
    private ClientApiElement joeFernerVertex;

    @Test
    public void testResolveTerm() throws IOException, ApiException {
        setupData();

        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);
        resolveTerm(visalloApi);
        assertHighlightedTextUpdatedWithResolvedEntity(visalloApi);
        assertDiff(visalloApi);
        visalloApi.logout();

        visalloApi = login(USERNAME_TEST_USER_2);
        addUserAuths(visalloApi, USERNAME_TEST_USER_2, "auth1");
        assertHighlightedTextDoesNotContainResolvedEntityForOtherUser(visalloApi);
        visalloApi.logout();

        visalloApi = login(USERNAME_TEST_USER_1);
        assertPublishAll(visalloApi, 2);
        visalloTestCluster.processGraphPropertyQueue();
        visalloApi.logout();

        visalloApi = login(USERNAME_TEST_USER_2);
        assertHighlightedTextContainResolvedEntityForOtherUser(visalloApi);
        visalloApi.logout();

        visalloApi = login(USERNAME_TEST_USER_1);
        resolveAndUnresolveTerm(visalloApi);
        visalloApi.logout();
    }

    public void setupData() throws ApiException, IOException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);
        addUserAuths(visalloApi, USERNAME_TEST_USER_1, "auth1");

        ClientApiArtifactImportResponse artifact = visalloApi.getVertexApi().importFile("auth1", "test.txt", new ByteArrayInputStream("Joe Ferner knows David Singley.".getBytes()));
        assertEquals(1, artifact.getVertexIds().size());
        artifactVertexId = artifact.getVertexIds().get(0);
        assertNotNull(artifactVertexId);

        visalloTestCluster.processGraphPropertyQueue();

        joeFernerVertex = visalloApi.getVertexApi().create(TestOntology.CONCEPT_PERSON, "auth1", "justification");
        visalloApi.getVertexApi().setProperty(joeFernerVertex.getId(), TEST_MULTI_VALUE_KEY, VisalloProperties.TITLE.getPropertyName(), "Joe Ferner", "auth1", "test", null, null);

        visalloTestCluster.processGraphPropertyQueue();

        assertPublishAll(visalloApi, 14);

        visalloApi.logout();
    }

    public void resolveTerm(VisalloApi visalloApi) throws ApiException {
        int entityStartOffset = "".length();
        int entityEndOffset = entityStartOffset + "Joe Ferner".length();
        visalloApi.getVertexApi().resolveTerm(
                artifactVertexId,
                TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY,
                entityStartOffset, entityEndOffset,
                "Joe Ferner",
                TestOntology.CONCEPT_PERSON,
                "auth1",
                joeFernerVertex.getId(),
                "test",
                null);
    }

    public void assertHighlightedTextUpdatedWithResolvedEntity(VisalloApi visalloApi) throws ApiException {
        String highlightedText = visalloApi.getVertexApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        LOGGER.info("%s", highlightedText);
        assertTrue("highlightedText did not contain string: " + highlightedText, highlightedText.contains("resolvedToVertexId&quot;:&quot;" + joeFernerVertex.getId() + "&quot;"));
    }

    public void assertDiff(VisalloApi visalloApi) throws ApiException {
        ClientApiWorkspaceDiff diff;
        diff = visalloApi.getWorkspaceApi().getDiff();
        LOGGER.info("assertDiff: %s", diff.toString());
        assertEquals(2, diff.getDiffs().size());
        String edgeId = null;
        boolean foundEdgeDiffItem = false;
        boolean foundEdgeVisibilityJsonDiffItem = false;
        for (ClientApiWorkspaceDiff.Item workspaceDiffItem : diff.getDiffs()) {
            if (workspaceDiffItem instanceof ClientApiWorkspaceDiff.EdgeItem) {
                foundEdgeDiffItem = true;
                edgeId = ((ClientApiWorkspaceDiff.EdgeItem) workspaceDiffItem).getEdgeId();
            }
        }
        for (ClientApiWorkspaceDiff.Item workspaceDiffItem : diff.getDiffs()) {
            if (workspaceDiffItem instanceof ClientApiWorkspaceDiff.PropertyItem &&
                    ((ClientApiWorkspaceDiff.PropertyItem) workspaceDiffItem).getElementId().equals(edgeId) &&
                    ((ClientApiWorkspaceDiff.PropertyItem) workspaceDiffItem).getElementType().equals("edge")) {
                foundEdgeVisibilityJsonDiffItem = true;
            }
        }
        assertTrue("foundEdgeDiffItem", foundEdgeDiffItem);
        assertTrue("foundEdgeVisibilityJsonDiffItem", foundEdgeVisibilityJsonDiffItem);
    }

    private void assertHighlightedTextDoesNotContainResolvedEntityForOtherUser(VisalloApi visalloApi) throws ApiException {
        String highlightedText = visalloApi.getVertexApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        LOGGER.info("%s", highlightedText);
        assertFalse("highlightedText contained string: " + highlightedText, highlightedText.contains("resolvedToVertexId&quot;:&quot;" + joeFernerVertex.getId() + "&quot;"));
    }

    private void assertHighlightedTextContainResolvedEntityForOtherUser(VisalloApi visalloApi) throws ApiException {
        String highlightedText = visalloApi.getVertexApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        LOGGER.info("%s", highlightedText);
        assertTrue("highlightedText does not contain string: " + highlightedText, highlightedText.contains("resolvedToVertexId&quot;:&quot;" + joeFernerVertex.getId() + "&quot;"));
    }

    private void resolveAndUnresolveTerm(VisalloApi visalloApi) throws ApiException {
        int entityStartOffset = "Joe Ferner knows ".length();
        int entityEndOffset = entityStartOffset + "David Singley".length();
        String sign = "David Singley";
        visalloApi.getVertexApi().resolveTerm(
                artifactVertexId,
                TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY,
                entityStartOffset, entityEndOffset,
                sign,
                TestOntology.CONCEPT_PERSON,
                "auth1",
                joeFernerVertex.getId(),
                "test",
                null);

        ClientApiTermMentionsResponse termMentions = visalloApi.getVertexApi().getTermMentions(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY, VisalloProperties.TEXT.getPropertyName());
        LOGGER.info("termMentions: %s", termMentions.toString());
        assertEquals(4, termMentions.getTermMentions().size());
        ClientApiElement davidSingleyTermMention = findDavidSingleyTermMention(termMentions);
        LOGGER.info("termMention: %s", davidSingleyTermMention.toString());

        String highlightedText = visalloApi.getVertexApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        LOGGER.info("highlightedText: %s", highlightedText);
        ClientApiProperty davidSingleyEdgeId = getProperty(davidSingleyTermMention.getProperties(), "", "http://visallo.org/termMention#resolvedEdgeId");
        String davidSingleyEdgeIdValue = (String) davidSingleyEdgeId.getValue();
        assertTrue("highlightedText invalid: " + highlightedText, highlightedText.contains(">David Singley<") && highlightedText.contains(davidSingleyEdgeIdValue));

        visalloApi.getVertexApi().unresolveTerm(davidSingleyTermMention.getId());

        termMentions = visalloApi.getVertexApi().getTermMentions(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY, VisalloProperties.TEXT.getPropertyName());
        LOGGER.info("termMentions: %s", termMentions.toString());
        assertEquals(3, termMentions.getTermMentions().size());

        highlightedText = visalloApi.getVertexApi().getHighlightedText(artifactVertexId, TikaTextExtractorGraphPropertyWorker.MULTI_VALUE_KEY);
        LOGGER.info("highlightedText: %s", highlightedText);
        assertTrue("highlightedText invalid: " + highlightedText, highlightedText.contains(">David Singley<") && !highlightedText.contains(davidSingleyEdgeIdValue));
    }

    private ClientApiElement findDavidSingleyTermMention(ClientApiTermMentionsResponse termMentions) {
        for (ClientApiElement termMention : termMentions.getTermMentions()) {
            for (ClientApiProperty property : termMention.getProperties()) {
                if (property.getName().equals(VisalloProperties.TERM_MENTION_TITLE.getPropertyName())) {
                    if ("David Singley".equals(property.getValue())) {
                        return termMention;
                    }
                }
            }
        }
        throw new RuntimeException("Could not find 'David Singley' in term mentions");
    }
}
