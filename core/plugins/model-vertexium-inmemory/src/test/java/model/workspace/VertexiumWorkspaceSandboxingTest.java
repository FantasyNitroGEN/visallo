package model.workspace;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.web.clientapi.model.*;
import org.visallo.web.clientapi.model.ClientApiPublishItem.Action;
import org.visallo.web.clientapi.model.ClientApiWorkspaceDiff.PropertyItem;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * This test class covers workspace diff, publish, and undo functionality.
 */
@RunWith(Parameterized.class)
public class VertexiumWorkspaceSandboxingTest extends VertexiumWorkspaceRepositoryTestBase {
    private static final Locale LOCALE = Locale.US;
    private static final String TIME_ZONE = Calendar.getInstance().getTimeZone().getID();
    private static final String WORKSPACE_ID = "testWorkspaceId";
    private static final String SECRET_VISIBILITY_SOURCE = "secret";
    private static final Visibility SECRET_VISIBILITY =
            VISIBILITY_TRANSLATOR.toVisibility(new VisibilityJson(SECRET_VISIBILITY_SOURCE)).getVisibility();
    private static final String OTHER_VISIBILITY_SOURCE = "other";

    private Workspace workspace;
    private InMemoryAuthorizations workspaceAuthorizations;
    private String initialVisibilitySource;
    private Visibility initialVisibility;
    private Visibility initialWorkspaceViz;
    private Metadata initialMetadata;
    private Visibility secretWorkspaceViz;
    private Metadata secretMetadata;

    @Parameterized.Parameters
    public static Iterable<Object[]> initialVisibilitySources() {
        return Arrays.asList(new Object[][] {
                { DEFAULT_VISIBILITY.getVisibilityString() }, { OTHER_VISIBILITY_SOURCE }
        });
    }

    public VertexiumWorkspaceSandboxingTest(String initialVisibilitySource) {
        this.initialVisibilitySource = initialVisibilitySource;
        this.initialVisibility = VISIBILITY_TRANSLATOR.toVisibility(new VisibilityJson(initialVisibilitySource)).getVisibility();
    }

    @Before
    public void before() {
        entity1Vertex = graph.prepareVertex("entity1aId", initialVisibility)
                .addPropertyValue("key1", "prop1", "value1", new Metadata(), initialVisibility)
                .addPropertyValue("key9", "prop9", "value9", new Metadata(), initialVisibility)
                .save(NO_AUTHORIZATIONS);
        userRepository.addAuthorization(user1, OTHER_VISIBILITY_SOURCE, userRepository.getSystemUser());
        userRepository.addAuthorization(user1, SECRET_VISIBILITY_SOURCE, userRepository.getSystemUser());

        idGenerator.push(WORKSPACE_ID);
        workspace = workspaceRepository.add("testWorkspaceTitle", user1);
        String workspaceId = workspace.getWorkspaceId();
        workspaceAuthorizations = new InMemoryAuthorizations(workspaceId, SECRET_VISIBILITY_SOURCE, OTHER_VISIBILITY_SOURCE);

        VisibilityJson initialVisibilityJson = new VisibilityJson(initialVisibilitySource);
        initialVisibilityJson.addWorkspace(workspaceId);
        initialWorkspaceViz = VISIBILITY_TRANSLATOR.toVisibilityNoSuperUser(initialVisibilityJson);
        initialMetadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(initialMetadata, initialVisibilityJson, DEFAULT_VISIBILITY);

        VisibilityJson secretVisibilityJson = new VisibilityJson(SECRET_VISIBILITY_SOURCE);
        secretVisibilityJson.addWorkspace(workspaceId);
        secretWorkspaceViz = VISIBILITY_TRANSLATOR.toVisibilityNoSuperUser(secretVisibilityJson);
        secretMetadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(secretMetadata, secretVisibilityJson, DEFAULT_VISIBILITY);

        workspaceRepository.updateEntityOnWorkspace(workspace, entity1Vertex.getId(), true, GRAPH_POSITION, user1);

        when(termMentionRepository.findByVertexIdAndProperty(anyString(), anyString(), anyString(),
                any(Visibility.class), any(Authorizations.class)))
                .thenReturn(Collections.<Vertex>emptySet());

        when(termMentionRepository.findResolvedTo(anyString(), any(Authorizations.class)))
                .thenReturn(Collections.<Vertex>emptySet());
    }

    @Test
    public void getDiffWithNoChangesReturnsZeroDiffs() {
        assertEquals(0, getPropertyDiffsFromWorkspace().size());
    }

    @Test
    public void getDiffWithChangedPublicPropertyValueReturnsOneDiff() {
        changePublicPropertyValueOnWorkspace();

        List<PropertyItem> diffs = getPropertyDiffsFromWorkspace();

        assertEquals(1, diffs.size());
        assertChangedPropertyValueDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithChangedPublicPropertyVisibilityReturnsOneDiff() {
        changePublicPropertyVisibilityOnWorkspace();

        List<PropertyItem> diffs = getPropertyDiffsFromWorkspace();

        assertEquals(1, diffs.size());
        assertChangedPropertyVisibilityDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithChangedPublicPropertyValueAndVisibilityReturnsOneDiff() {
        changePublicPropertyValueAndVisibilityOnWorkspace();

        List<PropertyItem> diffs = getPropertyDiffsFromWorkspace();

        assertEquals(1, diffs.size());
        assertChangedPropertyValueAndVisibilityDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithChangedPublicPropertySameValueTwiceReturnsOneDiff() {
        changePublicPropertyValueOnWorkspace();
        changePublicPropertyValueOnWorkspace();

        List<PropertyItem> diffs = getPropertyDiffsFromWorkspace();

        assertEquals(1, diffs.size());
        assertChangedPropertyValueDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithChangedPublicPropertySameVisibilityTwiceReturnsOneDiff() {
        changePublicPropertyVisibilityOnWorkspace();
        changePublicPropertyVisibilityOnWorkspace();

        List<PropertyItem> diffs = getPropertyDiffsFromWorkspace();

        assertEquals(1, diffs.size());
        assertChangedPropertyVisibilityDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithChangedPublicPropertySameValueAndVisibilityTwiceReturnsOneDiff() {
        changePublicPropertyValueAndVisibilityOnWorkspace();
        changePublicPropertyValueAndVisibilityOnWorkspace();

        List<PropertyItem> diffs = getPropertyDiffsFromWorkspace();

        assertEquals(1, diffs.size());
        assertChangedPropertyValueAndVisibilityDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithHiddenPublicPropertyAndChangedPublicPropertyValueReturnsOneDiff() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyValueOnWorkspace();

        List<PropertyItem> diffs = getPropertyDiffsFromWorkspace();

        assertEquals(1, diffs.size());
        assertChangedPropertyValueDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithHiddenPublicPropertyAndChangedPublicPropertyVisibilityReturnsOneDiff() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyVisibilityOnWorkspace();

        List<PropertyItem> diffs = getPropertyDiffsFromWorkspace();

        assertEquals(1, diffs.size());
        assertChangedPropertyVisibilityDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithHiddenPublicPropertyAndChangedPublicPropertyValueAndVisibilityReturnsOneDiff() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyValueAndVisibilityOnWorkspace();

        List<PropertyItem> diffs = getPropertyDiffsFromWorkspace();

        assertEquals(1, diffs.size());
        assertChangedPropertyValueAndVisibilityDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithChangedPublicPropertyValueAndVisibilityInTwoStepsReturnsOneDiff() {
        changePublicPropertyValueOnWorkspace();
        changeNonPublicPropertyVisibilityOnWorkspace(); // changes only the visibility for the current property

        List<PropertyItem> diffs = getPropertyDiffsFromWorkspace();

        assertEquals(1, diffs.size());
        assertChangedPropertyValueAndVisibilityDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithNewPropertyReturnsOneDiff() {
        addNewPropertyOnWorkspace();

        List<PropertyItem> diffs = getPropertyDiffsFromWorkspace();

        assertEquals(1, diffs.size());
        assertNewPropertyDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithDeletedPropertyReturnsOneDiff() {
        deletePublicPropertyOnWorkspace();

        List<PropertyItem> diffs = getPropertyDiffsFromWorkspace();

        assertEquals(1, diffs.size());
        assertDeletedPropertyDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithThreeDifferentPropertyActionsReturnsThreeDiffs() {
        changePublicPropertyValueAndVisibilityOnWorkspace();
        addNewPropertyOnWorkspace();
        deletePublicPropertyOnWorkspace();

        List<PropertyItem> diffs = getPropertyDiffsFromWorkspace();

        assertEquals(3, diffs.size());
        assertChangedPropertyValueAndVisibilityDiff(diffs.get(0));
        assertNewPropertyDiff(diffs.get(1));
        assertDeletedPropertyDiff(diffs.get(2));
    }

    @Test
    public void undoWithNoChangesResultsInNoDiffs() {
        undoPropertyDiffsAndAssertNoDiffs(getPropertyDiffsFromWorkspace());
    }

    @Test
    public void undoWithChangedPublicPropertyValueResultsInNoDiffs() {
        changePublicPropertyValueOnWorkspace();

        undoPropertyDiffsAndAssertNoDiffs(getPropertyDiffsFromWorkspace());
    }

    @Test
    public void undoWithChangedPublicPropertyVisibilityResultsInNoDiffs() {
        changePublicPropertyVisibilityOnWorkspace();

        undoPropertyDiffsAndAssertNoDiffs(getPropertyDiffsFromWorkspace());
    }

    @Test
    public void undoWithChangedPublicPropertyValueAndVisibilityResultsInNoDiffs() {
        changePublicPropertyValueAndVisibilityOnWorkspace();

        undoPropertyDiffsAndAssertNoDiffs(getPropertyDiffsFromWorkspace());
    }

    @Test
    public void undoWithChangedPublicPropertySameValueTwiceResultsInNoDiffs() {
        changePublicPropertyValueOnWorkspace();
        changePublicPropertyValueOnWorkspace();

        undoPropertyDiffsAndAssertNoDiffs(getPropertyDiffsFromWorkspace());
    }

    @Test
    public void undoWithChangedPublicPropertySameVisibilityTwiceResultsInNoDiffs() {
        changePublicPropertyVisibilityOnWorkspace();
        changePublicPropertyVisibilityOnWorkspace();

        undoPropertyDiffsAndAssertNoDiffs(getPropertyDiffsFromWorkspace());
    }

    @Test
    public void undoWithChangedPublicPropertySameValueAndVisibilityTwiceResultsInNoDiffs() {
        changePublicPropertyValueAndVisibilityOnWorkspace();
        changePublicPropertyValueAndVisibilityOnWorkspace();

        undoPropertyDiffsAndAssertNoDiffs(getPropertyDiffsFromWorkspace());
    }

    @Test
    public void undoWithHiddenPublicPropertyAndChangedPublicPropertyValueResultsInNoDiffs() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyValueOnWorkspace();

        undoPropertyDiffsAndAssertNoDiffs(getPropertyDiffsFromWorkspace());
    }

    @Test
    public void undoWithHiddenPublicPropertyAndChangedPublicPropertyVisibilityResultsInNoDiffs() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyVisibilityOnWorkspace();

        undoPropertyDiffsAndAssertNoDiffs(getPropertyDiffsFromWorkspace());
    }

    @Test
    public void undoWithHiddenPublicPropertyAndChangedPublicPropertyValueAndVisibilityResultsInNoDiffs() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyValueAndVisibilityOnWorkspace();

        undoPropertyDiffsAndAssertNoDiffs(getPropertyDiffsFromWorkspace());
    }

    @Test
    public void undoWithChangedPublicPropertyValueAndVisibilityInTwoStepsResultsInNoDiffs() {
        changePublicPropertyValueOnWorkspace();
        changeNonPublicPropertyVisibilityOnWorkspace(); // changes only the visibility for the current property

        undoPropertyDiffsAndAssertNoDiffs(getPropertyDiffsFromWorkspace());
    }

    @Test
    public void undoWithNewPropertyResultsInNoDiffs() {
        addNewPropertyOnWorkspace();

        undoPropertyDiffsAndAssertNoDiffs(getPropertyDiffsFromWorkspace());
    }

    @Test
    public void undoWithDeletedPropertyResultsInNoDiffs() {
        deletePublicPropertyOnWorkspace();

        undoPropertyDiffsAndAssertNoDiffs(getPropertyDiffsFromWorkspace());
    }

    @Test
    public void undoWithThreeDifferentPropertyActionsResultsInNoDiffs() {
        changePublicPropertyValueAndVisibilityOnWorkspace();
        addNewPropertyOnWorkspace();
        deletePublicPropertyOnWorkspace();

        undoPropertyDiffsAndAssertNoDiffs(getPropertyDiffsFromWorkspace());
    }

    @Test
    public void publishWithChangedPublicPropertyValueSucceeds() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyValueOnWorkspace();
        publishWorkspace();
        assertChangedPropertyValuePublished();
    }

    @Test
    public void publishWithChangedPublicPropertyVisibilitySucceeds() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyVisibilityOnWorkspace();
        publishWorkspace();
        assertChangedPropertyVisibilityPublished();
    }

    @Test
    public void publishWithChangedPublicPropertyValueAndVisibilitySucceeds() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyValueAndVisibilityOnWorkspace();
        publishWorkspace();
        assertChangedPropertyValueAndVisibilityPublished();
    }

    private void publishWorkspace() {
        List<PropertyItem> diffs = getPropertyDiffsFromWorkspace();
        assertFalse(diffs.isEmpty());

        ClientApiPublishItem[] publishItems = new ClientApiPublishItem[diffs.size()];
        int i = 0;
        for (PropertyItem diff : diffs) {
            ClientApiPropertyPublishItem publishItem = new ClientApiPropertyPublishItem();
            publishItem.setElementId(diff.getElementId());
            publishItem.setKey(diff.getKey());
            publishItem.setName(diff.getName());
            publishItem.setVisibilityString(diff.getVisibilityString());
            publishItem.setAction(diff.isDeleted() ? Action.delete : Action.addOrUpdate);
            publishItems[i++] = publishItem;
        }

        ClientApiWorkspacePublishResponse response =
                workspaceRepository.publish(publishItems, workspace.getWorkspaceId(), workspaceAuthorizations);

        assertTrue(response.isSuccess());
        assertEquals(0, getPropertyDiffsFromWorkspace().size());
        entity1Vertex = graph.getVertex(entity1Vertex.getId(), workspaceAuthorizations);
    }

    private List<PropertyItem> getPropertyDiffsFromWorkspace() {
        List<PropertyItem> diffs = new ArrayList<>();
        ClientApiWorkspaceDiff workspaceDiff = workspaceRepository.getDiff(workspace, user1, LOCALE, TIME_ZONE);
        for (ClientApiWorkspaceDiff.Item diff : workspaceDiff.getDiffs()) {
            diffs.add((PropertyItem) diff);
        }
        return diffs;
    }

    private void undoPropertyDiffs(List<PropertyItem> diffs) {
        List<ClientApiUndoItem> undoItems = new ArrayList<>(diffs.size());
        for (PropertyItem diff : diffs) {
            ClientApiPropertyUndoItem item = new ClientApiPropertyUndoItem();
            item.setElementId(diff.getElementId());
            item.setVertexId(diff.getElementId());
            item.setKey(diff.getKey());
            item.setName(diff.getName());
            item.setVisibilityString(diff.getVisibilityString());
            item.setAction(diff.isDeleted() ? ClientApiUndoItem.Action.delete : ClientApiUndoItem.Action.addOrUpdate);
            undoItems.add(item);
        }
        ClientApiWorkspaceUndoResponse workspaceUndoResponse = new ClientApiWorkspaceUndoResponse();
        workspaceUndoHelper.undo(undoItems, workspaceUndoResponse, workspace.getWorkspaceId(), user1,
                workspaceAuthorizations);
        assertTrue(workspaceUndoResponse.isSuccess());
    }

    private void undoPropertyDiffsAndAssertNoDiffs(List<PropertyItem> diffs) {
        undoPropertyDiffs(diffs);
        assertEquals(0, getPropertyDiffsFromWorkspace().size());
    }

    private void markPublicPropertyHiddenOnWorkspace() {
        entity1Vertex.markPropertyHidden("key1", "prop1", initialVisibility, new Visibility(workspace.getWorkspaceId()),
                workspaceAuthorizations);
    }

    private void changePublicPropertyValueOnWorkspace() {
        entity1Vertex.prepareMutation()
                .addPropertyValue("key1", "prop1", "value1a", initialMetadata, initialWorkspaceViz)
                .save(workspaceAuthorizations);
    }

    private void assertChangedPropertyValueDiff(PropertyItem diff) {
        assertEquals("key1", diff.getKey());
        assertEquals("prop1", diff.getName());
        assertEquals("value1", diff.getOldData().get("value").asText());
        assertEquals("value1a", diff.getNewData().get("value").asText());
        assertVisibilityEquals(initialWorkspaceViz.getVisibilityString(), diff.getVisibilityString());
        assertEquals(SandboxStatus.PUBLIC_CHANGED, diff.getSandboxStatus());
        assertFalse(diff.isDeleted());
    }

    private void assertChangedPropertyValuePublished() {
        Property property = entity1Vertex.getProperty("key1", "prop1");
        assertEquals("value1a", property.getValue());
        assertVisibilityOnProperty(initialVisibility, property);
        assertFalse(property.isHidden(workspaceAuthorizations));
    }

    private void changePublicPropertyVisibilityOnWorkspace() {
        entity1Vertex.prepareMutation()
                .addPropertyValue("key1", "prop1", "value1", secretMetadata, secretWorkspaceViz)
                .save(workspaceAuthorizations);
    }

    private void changeNonPublicPropertyVisibilityOnWorkspace() {
        entity1Vertex.prepareMutation()
                .alterPropertyVisibility("key1", "prop1", secretWorkspaceViz)
                .save(workspaceAuthorizations);
    }

    private void assertChangedPropertyVisibilityDiff(PropertyItem diff) {
        assertEquals("key1", diff.getKey());
        assertEquals("prop1", diff.getName());
        assertEquals("value1", diff.getOldData().get("value").asText());
        assertEquals("value1", diff.getNewData().get("value").asText());
        assertVisibilityEquals(secretWorkspaceViz.getVisibilityString(), diff.getVisibilityString());
        assertEquals(SandboxStatus.PUBLIC_CHANGED, diff.getSandboxStatus());
        assertFalse(diff.isDeleted());
    }

    private void assertChangedPropertyVisibilityPublished() {
        Property property = entity1Vertex.getProperty("key1", "prop1");
        assertEquals("value1", property.getValue());
        assertVisibilityOnProperty(SECRET_VISIBILITY, property);
        assertFalse(property.isHidden(workspaceAuthorizations));
    }

    private void changePublicPropertyValueAndVisibilityOnWorkspace() {
        entity1Vertex.prepareMutation()
                .addPropertyValue("key1", "prop1", "value1a", secretMetadata, secretWorkspaceViz)
                .save(workspaceAuthorizations);
    }

    private void assertChangedPropertyValueAndVisibilityDiff(PropertyItem diff) {
        assertEquals("key1", diff.getKey());
        assertEquals("prop1", diff.getName());
        assertEquals("value1", diff.getOldData().get("value").asText());
        assertEquals("value1a", diff.getNewData().get("value").asText());
        assertVisibilityEquals(secretWorkspaceViz.getVisibilityString(), diff.getVisibilityString());
        assertEquals(SandboxStatus.PUBLIC_CHANGED, diff.getSandboxStatus());
        assertFalse(diff.isDeleted());
    }

    private void assertChangedPropertyValueAndVisibilityPublished() {
        Property property = entity1Vertex.getProperty("key1", "prop1");
        assertEquals("value1a", property.getValue());
        assertVisibilityOnProperty(SECRET_VISIBILITY, property);
        assertFalse(property.isHidden(workspaceAuthorizations));
    }

    private void addNewPropertyOnWorkspace() {
        entity1Vertex.prepareMutation()
                .addPropertyValue("key2", "prop2", "value2a", initialMetadata, initialWorkspaceViz)
                .save(workspaceAuthorizations);
    }

    private void assertNewPropertyDiff(PropertyItem diff) {
        assertEquals("key2", diff.getKey());
        assertEquals("prop2", diff.getName());
        assertNull(diff.getOldData());
        assertEquals("value2a", diff.getNewData().get("value").asText());
        assertVisibilityEquals(initialWorkspaceViz.getVisibilityString(), diff.getVisibilityString());
        assertEquals(SandboxStatus.PRIVATE, diff.getSandboxStatus());
        assertFalse(diff.isDeleted());
    }

    private void deletePublicPropertyOnWorkspace() {
        entity1Vertex.markPropertyHidden("key9", "prop9", initialVisibility, new Visibility(workspace.getWorkspaceId()),
                workspaceAuthorizations);
    }

    private void assertDeletedPropertyDiff(PropertyItem diff) {
        assertEquals("key9", diff.getKey());
        assertEquals("prop9", diff.getName());
        assertNull(diff.getOldData());
        assertEquals("value9", diff.getNewData().get("value").asText());
        assertVisibilityEquals(initialVisibility.getVisibilityString(), diff.getVisibilityString());
        assertEquals(SandboxStatus.PUBLIC, diff.getSandboxStatus());
        assertTrue(diff.isDeleted());
    }

    private static void assertVisibilityOnProperty(Visibility expectedVisibility, Property property) {
        String expectedVisibilityString = expectedVisibility.getVisibilityString();
        Visibility actualVisibility = property.getVisibility();
        String actualVisibilityString = actualVisibility.getVisibilityString();
        assertVisibilityEquals(expectedVisibilityString, actualVisibilityString);

        Metadata metadata = property.getMetadata();
        assertNotNull(metadata);

        VisibilityJson expectedVisibilityJson = new VisibilityJson(extractVisibilitySource(expectedVisibilityString));
        VisibilityJson actualVisibilityJson = VisalloProperties.VISIBILITY_JSON_METADATA.getMetadataValue(metadata);
        assertNotNull(actualVisibilityJson);
        assertEquals(expectedVisibilityJson, actualVisibilityJson);
    }

    private static void assertVisibilityEquals(String expected, String actual) {
        assertEquals(stripParenthesis(expected), stripParenthesis(actual));
    }

    private static String stripParenthesis(String s) {
        return s.replaceAll("\\(|\\)", "");
    }

    private static String extractVisibilitySource(String visibilityString) {
        return stripParenthesis(visibilityString)
                .replaceAll("&|\\|", "")
                .replace(VisalloVisibility.SUPER_USER_VISIBILITY_STRING, "");
    }
}
