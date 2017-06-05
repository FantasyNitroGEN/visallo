package model.workspace;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.visallo.core.formula.FormulaEvaluator;
import org.visallo.core.ingest.graphProperty.ElementOrPropertyStatus;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.vertexium.model.ontology.InMemoryConcept;
import org.visallo.vertexium.model.ontology.InMemoryOntologyProperty;
import org.visallo.web.clientapi.model.*;
import org.visallo.web.clientapi.model.ClientApiPublishItem.Action;
import org.visallo.web.clientapi.model.ClientApiWorkspaceDiff.EdgeItem;
import org.visallo.web.clientapi.model.ClientApiWorkspaceDiff.PropertyItem;
import org.visallo.web.clientapi.model.ClientApiWorkspaceDiff.VertexItem;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.visallo.core.util.StreamUtil.stream;

/**
 * This test class covers workspace diff, publish, and undo functionality.
 */
@SuppressWarnings("Duplicates")
@RunWith(Parameterized.class)
public class VertexiumWorkspaceSandboxingTest extends VertexiumWorkspaceRepositoryTestBase {
    private static final Locale LOCALE = Locale.US;
    private static final String TIME_ZONE = Calendar.getInstance().getTimeZone().getID();
    private static final String WORKSPACE_ID = "testWorkspaceId";
    private static final String SECRET_VISIBILITY_SOURCE = "secret";
    private static final Visibility SECRET_VISIBILITY =
            VISIBILITY_TRANSLATOR.toVisibility(new VisibilityJson(SECRET_VISIBILITY_SOURCE)).getVisibility();
    private static final String OTHER_VISIBILITY_SOURCE = "other";
    private static final String VERTEX_TITLE = "The Title";
    private static final String VERTEX_CONCEPT_TYPE = "The Concept Type";

    private Workspace workspace;
    private InMemoryAuthorizations workspaceAuthorizations;
    private String initialVisibilitySource;
    private Visibility initialVisibility;
    private Visibility initialWorkspaceViz;
    private Metadata initialMetadata;
    private Visibility secretWorkspaceViz;
    private Metadata secretMetadata;
    private VisibilityJson initialVisibilityJson;
    private FormulaEvaluator.UserContext userContext;

    @Parameterized.Parameters
    public static Iterable<Object[]> initialVisibilitySources() {
        return Arrays.asList(new Object[][]{
                {DEFAULT_VISIBILITY.getVisibilityString()}, {OTHER_VISIBILITY_SOURCE}
        });
    }

    public VertexiumWorkspaceSandboxingTest(String initialVisibilitySource) {
        this.initialVisibilitySource = initialVisibilitySource;
        this.initialVisibility = VISIBILITY_TRANSLATOR.toVisibility(new VisibilityJson(initialVisibilitySource)).getVisibility();
    }

    @Before
    public void before() throws Exception {
        super.before();

        entity1Vertex = graph.prepareVertex("entity1aId", initialVisibility)
                .addPropertyValue("key1", "prop1", "value1", new Metadata(), initialVisibility)
                .addPropertyValue("key9", "prop9", "value9", new Metadata(), initialVisibility)
                .save(NO_AUTHORIZATIONS);

        authorizationRepository.addAuthorization(user1, SECRET_VISIBILITY_SOURCE, userRepository.getSystemUser());
        authorizationRepository.addAuthorization(user1, OTHER_VISIBILITY_SOURCE, userRepository.getSystemUser());

        idGenerator.push(WORKSPACE_ID);
        workspace = workspaceRepository.add("testWorkspaceTitle", user1);
        String workspaceId = workspace.getWorkspaceId();
        workspaceAuthorizations = new InMemoryAuthorizations(
                workspaceId,
                SECRET_VISIBILITY_SOURCE,
                OTHER_VISIBILITY_SOURCE
        );

        // important: reload vertex with all authorizations
        entity1Vertex = graph.getVertex(entity1Vertex.getId(), workspaceAuthorizations);

        initialVisibilityJson = new VisibilityJson(initialVisibilitySource);
        initialVisibilityJson.addWorkspace(workspaceId);
        initialWorkspaceViz = VISIBILITY_TRANSLATOR.toVisibilityNoSuperUser(initialVisibilityJson);
        initialMetadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(
                initialMetadata,
                initialVisibilityJson,
                DEFAULT_VISIBILITY
        );

        VisibilityJson secretVisibilityJson = new VisibilityJson(SECRET_VISIBILITY_SOURCE);
        secretVisibilityJson.addWorkspace(workspaceId);
        secretWorkspaceViz = VISIBILITY_TRANSLATOR.toVisibilityNoSuperUser(secretVisibilityJson);
        secretMetadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(
                secretMetadata,
                secretVisibilityJson,
                DEFAULT_VISIBILITY
        );

        workspaceRepository.updateEntityOnWorkspace(workspace, entity1Vertex.getId(), user1);

        when(termMentionRepository.findByVertexId(anyString(), any(Authorizations.class)))
                .thenReturn(Collections.emptyList());
        when(termMentionRepository.findByVertexIdAndProperty(anyString(), anyString(), anyString(),
                                                             any(Visibility.class), any(Authorizations.class)
        ))
                .thenReturn(Collections.emptyList());
        when(termMentionRepository.findResolvedTo(anyString(), any(Authorizations.class)))
                .thenReturn(Collections.emptyList());
        when(termMentionRepository.findByEdgeId(anyString(), anyString(), any(Authorizations.class)))
                .thenReturn(Collections.emptyList());
        when(termMentionRepository.findByEdgeForEdge(any(Edge.class), any(Authorizations.class)))
                .thenReturn(Collections.emptyList());
        when(ontologyRepository.getPropertyByIRI(VisalloProperties.VISIBILITY_JSON.getPropertyName()))
                .thenReturn(new InMemoryOntologyProperty());

        userContext = new FormulaEvaluator.UserContext(LOCALE, null, TIME_ZONE, workspaceId);

        when(ontologyRepository.getConceptByIRI(VERTEX_CONCEPT_TYPE, user1, workspace.getWorkspaceId()))
            .thenReturn(new InMemoryConcept(VERTEX_CONCEPT_TYPE, OntologyRepository.ENTITY_CONCEPT_IRI, null));
    }

    @Test
    public void getDiffWithNoChangesReturnsZeroDiffs() {
        assertNoDiffs();
    }

    @Test
    public void getDiffWithChangedPublicPropertyValueReturnsOneDiff() {
        changePublicPropertyValueOnWorkspace();

        List<PropertyItem> diffs = getDiffsFromWorkspace(PropertyItem.class);

        assertEquals(1, diffs.size());
        assertChangedPropertyValueDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithChangedPublicPropertyVisibilityReturnsOneDiff() {
        changePublicPropertyVisibilityOnWorkspace();

        List<PropertyItem> diffs = getDiffsFromWorkspace(PropertyItem.class);

        assertEquals(1, diffs.size());
        assertChangedPropertyVisibilityDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithChangedPublicPropertyValueAndVisibilityReturnsOneDiff() {
        changePublicPropertyValueAndVisibilityOnWorkspace();

        List<PropertyItem> diffs = getDiffsFromWorkspace(PropertyItem.class);

        assertEquals(1, diffs.size());
        assertChangedPropertyValueAndVisibilityDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithChangedPublicPropertySameValueTwiceReturnsOneDiff() {
        changePublicPropertyValueOnWorkspace();
        changePublicPropertyValueOnWorkspace();

        List<PropertyItem> diffs = getDiffsFromWorkspace(PropertyItem.class);

        assertEquals(1, diffs.size());
        assertChangedPropertyValueDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithChangedPublicPropertySameVisibilityTwiceReturnsOneDiff() {
        changePublicPropertyVisibilityOnWorkspace();
        changePublicPropertyVisibilityOnWorkspace();

        List<PropertyItem> diffs = getDiffsFromWorkspace(PropertyItem.class);

        assertEquals(1, diffs.size());
        assertChangedPropertyVisibilityDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithChangedPublicPropertySameValueAndVisibilityTwiceReturnsOneDiff() {
        changePublicPropertyValueAndVisibilityOnWorkspace();
        changePublicPropertyValueAndVisibilityOnWorkspace();

        List<PropertyItem> diffs = getDiffsFromWorkspace(PropertyItem.class);

        assertEquals(1, diffs.size());
        assertChangedPropertyValueAndVisibilityDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithHiddenPublicPropertyAndChangedPublicPropertyValueReturnsOneDiff() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyValueOnWorkspace();

        List<PropertyItem> diffs = getDiffsFromWorkspace(PropertyItem.class);

        assertEquals(1, diffs.size());
        assertChangedPropertyValueDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithHiddenPublicPropertyAndChangedPublicPropertyVisibilityReturnsOneDiff() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyVisibilityOnWorkspace();

        List<PropertyItem> diffs = getDiffsFromWorkspace(PropertyItem.class);

        assertEquals(1, diffs.size());
        assertChangedPropertyVisibilityDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithHiddenPublicPropertyAndChangedPublicPropertyValueAndVisibilityReturnsOneDiff() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyValueAndVisibilityOnWorkspace();

        List<PropertyItem> diffs = getDiffsFromWorkspace(PropertyItem.class);

        assertEquals(1, diffs.size());
        assertChangedPropertyValueAndVisibilityDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithChangedPublicPropertyValueAndVisibilityInTwoStepsReturnsOneDiff() {
        changePublicPropertyValueOnWorkspace();
        changeNonPublicPropertyVisibilityOnWorkspace(); // changes only the visibility for the current property

        List<PropertyItem> diffs = getDiffsFromWorkspace(PropertyItem.class);

        assertEquals(1, diffs.size());
        assertChangedPropertyValueAndVisibilityDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithNewPropertyReturnsOneDiff() {
        addNewPropertyOnWorkspace();

        List<PropertyItem> diffs = getDiffsFromWorkspace(PropertyItem.class);

        assertEquals(1, diffs.size());
        assertNewPropertyDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithDeletedPropertyReturnsOneDiff() {
        deletePublicPropertyOnWorkspace();

        List<PropertyItem> diffs = getDiffsFromWorkspace(PropertyItem.class);

        assertEquals(1, diffs.size());
        assertDeletedPropertyDiff(diffs.get(0));
    }

    @Test
    public void getDiffWithThreeDifferentPropertyActionsReturnsThreeDiffs() {
        changePublicPropertyValueAndVisibilityOnWorkspace();
        addNewPropertyOnWorkspace();
        deletePublicPropertyOnWorkspace();

        List<PropertyItem> diffs = getDiffsFromWorkspace(PropertyItem.class);

        assertEquals(3, diffs.size());
        assertChangedPropertyValueAndVisibilityDiff(diffs.get(0));
        assertNewPropertyDiff(diffs.get(1));
        assertDeletedPropertyDiff(diffs.get(2));
    }

    @Test
    public void undoWithNoChangesResultsInNoDiffs() {
        undoPropertyDiffs(getDiffsFromWorkspace(PropertyItem.class));
    }

    @Test
    public void undoWithChangedPublicPropertyValueResultsInNoDiffs() {
        changePublicPropertyValueOnWorkspace();

        undoPropertyDiffs(getDiffsFromWorkspace(PropertyItem.class));
    }

    @Test
    public void undoWithChangedPublicPropertyVisibilityResultsInNoDiffs() {
        changePublicPropertyVisibilityOnWorkspace();

        undoPropertyDiffs(getDiffsFromWorkspace(PropertyItem.class));
    }

    @Test
    public void undoWithChangedPublicPropertyValueAndVisibilityResultsInNoDiffs() {
        changePublicPropertyValueAndVisibilityOnWorkspace();

        undoPropertyDiffs(getDiffsFromWorkspace(PropertyItem.class));
    }

    @Test
    public void undoWithChangedPublicPropertySameValueTwiceResultsInNoDiffs() {
        changePublicPropertyValueOnWorkspace();
        changePublicPropertyValueOnWorkspace();

        undoPropertyDiffs(getDiffsFromWorkspace(PropertyItem.class));
    }

    @Test
    public void undoWithChangedPublicPropertySameVisibilityTwiceResultsInNoDiffs() {
        changePublicPropertyVisibilityOnWorkspace();
        changePublicPropertyVisibilityOnWorkspace();

        undoPropertyDiffs(getDiffsFromWorkspace(PropertyItem.class));
    }

    @Test
    public void undoWithChangedPublicPropertySameValueAndVisibilityTwiceResultsInNoDiffs() {
        changePublicPropertyValueAndVisibilityOnWorkspace();
        changePublicPropertyValueAndVisibilityOnWorkspace();

        undoPropertyDiffs(getDiffsFromWorkspace(PropertyItem.class));
    }

    @Test
    public void undoWithHiddenPublicPropertyAndChangedPublicPropertyValueResultsInNoDiffs() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyValueOnWorkspace();

        undoPropertyDiffs(getDiffsFromWorkspace(PropertyItem.class));
    }

    @Test
    public void undoWithHiddenPublicPropertyAndChangedPublicPropertyVisibilityResultsInNoDiffs() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyVisibilityOnWorkspace();

        undoPropertyDiffs(getDiffsFromWorkspace(PropertyItem.class));
    }

    @Test
    public void undoWithHiddenPublicPropertyAndChangedPublicPropertyValueAndVisibilityResultsInNoDiffs() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyValueAndVisibilityOnWorkspace();

        undoPropertyDiffs(getDiffsFromWorkspace(PropertyItem.class));
    }

    @Test
    public void undoWithChangedPublicPropertyValueAndVisibilityInTwoStepsResultsInNoDiffs() {
        changePublicPropertyValueOnWorkspace();
        changeNonPublicPropertyVisibilityOnWorkspace(); // changes only the visibility for the current property

        undoPropertyDiffs(getDiffsFromWorkspace(PropertyItem.class));
    }

    @Test
    public void undoWithNewPropertyResultsInNoDiffs() {
        addNewPropertyOnWorkspace();

        undoPropertyDiffs(getDiffsFromWorkspace(PropertyItem.class));
    }

    @Test
    public void undoWithDeletedPropertyResultsInNoDiffs() {
        deletePublicPropertyOnWorkspace();

        undoPropertyDiffs(getDiffsFromWorkspace(PropertyItem.class));
    }

    @Test
    public void undoWithThreeDifferentPropertyActionsResultsInNoDiffs() {
        changePublicPropertyValueAndVisibilityOnWorkspace();
        addNewPropertyOnWorkspace();
        deletePublicPropertyOnWorkspace();

        undoPropertyDiffs(getDiffsFromWorkspace(PropertyItem.class));
    }

    @Test
    public void publishWithChangedPublicPropertyValueSucceeds() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyValueOnWorkspace();
        publishAllWorkspaceDiffs();
        assertChangedPropertyValuePublished();
    }

    @Test
    public void publishWithChangedPublicPropertyVisibilitySucceeds() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyVisibilityOnWorkspace();
        publishAllWorkspaceDiffs();
        assertChangedPropertyVisibilityPublished();
    }

    @Test
    public void publishWithChangedPublicPropertyValueAndVisibilitySucceeds() {
        markPublicPropertyHiddenOnWorkspace();
        changePublicPropertyValueAndVisibilityOnWorkspace();
        publishAllWorkspaceDiffs();
        assertChangedPropertyValueAndVisibilityPublished();
    }

    @Test
    public void publishNewVertexSucceeds() {
        Vertex vertex = newVertexOnWorkspace();

        when(formulaEvaluator.evaluateTitleFormula(
                any(Element.class), any(FormulaEvaluator.UserContext.class), any(Authorizations.class)))
                .thenReturn(VERTEX_TITLE);

        List<VertexItem> vertexDiffs = getDiffsFromWorkspace(VertexItem.class);
        assertEquals(1, vertexDiffs.size());
        VertexItem vertexDiff = vertexDiffs.get(0);
        assertEquals(vertex.getId(), vertexDiff.getVertexId());
        assertEquals(VERTEX_CONCEPT_TYPE, vertexDiff.getConceptType());
        assertEquals(VERTEX_TITLE, vertexDiff.getTitle());
        assertEquals("VertexDiffItem", vertexDiff.getType());
        assertEquals(SandboxStatus.PRIVATE, vertexDiff.getSandboxStatus());
        assertFalse(vertexDiff.isDeleted());
        assertEquals(initialVisibilityJson.toString(), vertexDiff.getVisibilityJson().get("value").toString());

        List<PropertyItem> propertyDiffs = getDiffsFromWorkspace(PropertyItem.class);
        assertEquals(2, propertyDiffs.size()); // conceptType and title

        publishAllWorkspaceDiffs();

        vertex = graph.getVertex(vertex.getId(), workspaceAuthorizations);
        assertEquals(vertex.getId(), vertexDiff.getVertexId());
        assertEquals(
                VERTEX_CONCEPT_TYPE,
                vertex.getProperty(VisalloProperties.CONCEPT_TYPE.getPropertyName(), initialVisibility).getValue()
        );
        assertEquals(VERTEX_TITLE, VisalloProperties.TITLE.getOnlyPropertyValue(vertex));
        assertEquals(
                VisibilityJson.removeFromAllWorkspace(initialVisibilityJson),
                VisalloProperties.VISIBILITY_JSON.getPropertyValue(vertex)
        );
        assertNull(vertex.getProperty(VisalloProperties.CONCEPT_TYPE.getPropertyName(), initialWorkspaceViz));
        assertNull(vertex.getProperty(VisalloProperties.TITLE.getPropertyName(), initialWorkspaceViz));
        assertNull(vertex.getProperty(VisalloProperties.VISIBILITY_JSON.getPropertyName(), initialWorkspaceViz));
    }

    @Test
    public void publishDeleteVertexSucceeds() {
        Vertex vertex = newVertexOnWorkspace();

        // publish the new vertex
        publishAllWorkspaceDiffs();
        vertex = graph.getVertex(vertex.getId(), workspaceAuthorizations);

        // delete the vertex
        SandboxStatus sandboxStatus = SandboxStatusUtil.getSandboxStatus(vertex, workspace.getWorkspaceId());
        assertEquals(SandboxStatus.PUBLIC, sandboxStatus);
        workspaceHelper.deleteVertex(
                vertex, workspace.getWorkspaceId(), true, Priority.HIGH, workspaceAuthorizations, user1);

        List<VertexItem> vertexDiffs = getDiffsFromWorkspace(VertexItem.class);

        assertEquals(1, vertexDiffs.size());
        VertexItem vertexDiff = vertexDiffs.get(0);
        assertEquals(vertex.getId(), vertexDiff.getVertexId());
        assertEquals(VERTEX_CONCEPT_TYPE, vertexDiff.getConceptType());
        assertNull(vertexDiff.getTitle());
        assertEquals("VertexDiffItem", vertexDiff.getType());
        assertEquals(SandboxStatus.PUBLIC, vertexDiff.getSandboxStatus());
        assertTrue(vertexDiff.isDeleted());
        assertEquals(
                new VisibilityJson(initialVisibilitySource).toString(),
                vertexDiff.getVisibilityJson().get("value").toString()
        );

        assertEquals(0, getDiffsFromWorkspace(PropertyItem.class).size());
        assertEquals(0, getDiffsFromWorkspace(EdgeItem.class).size());

        // publish the vertex deletion
        publishAllWorkspaceDiffs();

        assertNull(graph.getVertex(vertex.getId(), workspaceAuthorizations));
    }

    @Test
    public void undoDeleteVertexSucceeds() {
        Vertex vertex = newVertexOnWorkspace();
        String vertexId = vertex.getId();

        // publish the new vertex
        publishAllWorkspaceDiffs();
        vertex = graph.getVertex(vertexId, workspaceAuthorizations);

        // delete the vertex
        SandboxStatus sandboxStatus = SandboxStatusUtil.getSandboxStatus(vertex, workspace.getWorkspaceId());
        assertEquals(SandboxStatus.PUBLIC, sandboxStatus);
        workspaceHelper.deleteVertex(
                vertex, workspace.getWorkspaceId(), true, Priority.HIGH, workspaceAuthorizations, user1);
        assertNull(graph.getVertex(vertexId, workspaceAuthorizations));

        assertEquals(1, getDiffsFromWorkspace(VertexItem.class).size());
        assertEquals(0, getDiffsFromWorkspace(PropertyItem.class).size());
        assertEquals(0, getDiffsFromWorkspace(EdgeItem.class).size());

        // undo the vertex deletion
        undoAllWorkspaceDiffs();

        assertEquals(0, getDiffsFromWorkspace(VertexItem.class).size());
        assertEquals(0, getDiffsFromWorkspace(PropertyItem.class).size());
        assertEquals(0, getDiffsFromWorkspace(EdgeItem.class).size());

        vertex = graph.getVertex(vertexId, workspaceAuthorizations);
        assertEquals(vertexId, vertex.getId());
        assertEquals(3, stream(vertex.getProperties()).count());
        assertEquals(VERTEX_CONCEPT_TYPE, VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex));
        assertEquals(VERTEX_TITLE, VisalloProperties.TITLE.getPropertyValue(vertex, ""));
        assertEquals(
                new VisibilityJson(initialVisibilitySource),
                VisalloProperties.VISIBILITY_JSON.getPropertyValue(vertex)
        );
    }

    @Test
    public void recreatedVertexWithSameIdAfterUndoShouldNotHaveOldProperties() {
        Metadata propertyMetadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(propertyMetadata, initialVisibilityJson,
                                                               VISIBILITY_TRANSLATOR.getDefaultVisibility()
        );

        ElementBuilder<Vertex> vertexBuilder = graph.prepareVertex("v1", initialVisibility)
                .addPropertyValue("k1", "p1", "v1", propertyMetadata, initialVisibility)
                .addPropertyValue("k2", "p2", "v2", propertyMetadata, initialVisibility);
        VisalloProperties.VISIBILITY_JSON.setProperty(
                vertexBuilder, initialVisibilityJson, propertyMetadata, new Visibility(""));
        Vertex v1 = vertexBuilder.save(workspaceAuthorizations);
        graph.flush();

        workspaceRepository.updateEntityOnWorkspace(workspace, v1.getId(), user1);
        graph.flush();

        String vertexId = v1.getId();
        assertNotNull(graph.getVertex(vertexId, workspaceAuthorizations));
        List<ClientApiWorkspaceDiff.Item> diffs = getDiffsFromWorkspace();
        assertEquals(4, diffs.size());
        undoWorkspaceDiffs(diffs);
        assertNull(graph.getVertex(vertexId, workspaceAuthorizations));

        vertexBuilder = graph.prepareVertex("v1", initialVisibility)
                .addPropertyValue("k3", "p1", "v1", propertyMetadata, initialVisibility)
                .addPropertyValue("k4", "p2", "v2", propertyMetadata, initialVisibility);
        VisalloProperties.VISIBILITY_JSON.setProperty(
                vertexBuilder, initialVisibilityJson, propertyMetadata, new Visibility(""));
        vertexBuilder.save(workspaceAuthorizations);
        graph.flush();

        workspaceRepository.updateEntityOnWorkspace(workspace, v1.getId(), user1);
        graph.flush();

        assertNotNull(graph.getVertex(vertexId, workspaceAuthorizations));
        diffs = getDiffsFromWorkspace();
        assertEquals(4, diffs.size());
        undoWorkspaceDiffs(diffs);
        assertNull(graph.getVertex(vertexId, workspaceAuthorizations));
    }

    @Test
    public void sandboxedPublicPropertyDeletionShouldPushHiddenStatus() {
        changePublicPropertyValueOnWorkspace();
        graph.flush();
        publishAllWorkspaceDiffs();

        entity1Vertex = graph.getVertex(entity1Vertex.getId(), workspaceAuthorizations);
        Property property = entity1Vertex.getProperty("key1", "prop1");
        assertNotNull(property);
        workspaceHelper.deleteProperty(
                entity1Vertex,
                property,
                true,
                WORKSPACE_ID,
                Priority.HIGH,
                workspaceAuthorizations
        );
        verify(workQueueRepository).pushGraphPropertyQueueHiddenOrDeleted(
                eq(entity1Vertex),
                eq(property),
                eq(ElementOrPropertyStatus.HIDDEN),
                any(Long.class),
                eq(WORKSPACE_ID),
                eq(Priority.HIGH)
        );
    }

    @Test
    public void sandboxedPropertyUndoShouldPushDeletionStatus() {
        changePublicPropertyValueOnWorkspace();
        List<ClientApiWorkspaceDiff.PropertyItem> diffs = getDiffsFromWorkspace(ClientApiWorkspaceDiff.PropertyItem.class);
        undoPropertyDiffs(diffs);
        verify(workQueueRepository).pushUndoSandboxProperty(
                eq(entity1Vertex),
                eq("key1"),
                eq("prop1"),
                any(Long.class),
                eq(Priority.HIGH)
        );
    }

    @Test
    public void sandboxedPublicPropertyDeletionUndoShouldPushUnhiddenStatus() {
        markPublicPropertyHiddenOnWorkspace();
        List<ClientApiWorkspaceDiff.PropertyItem> diffs = getDiffsFromWorkspace(ClientApiWorkspaceDiff.PropertyItem.class);
        undoPropertyDiffs(diffs);
        verify(workQueueRepository).pushUndoPublicPropertyDeletion(
                eq(entity1Vertex),
                eq("key1"),
                eq("prop1"),
                eq(Priority.HIGH)
        );
    }

    @Test
    public void publishedPublicPropertyDeletionShouldPushDeletionStatus() {
        markPublicPropertyHiddenOnWorkspace();
        publishAllWorkspaceDiffs();
        verify(workQueueRepository).pushPublishedPropertyDeletion(
                eq(entity1Vertex),
                eq("key1"),
                eq("prop1"),
                any(Long.class),
                eq(Priority.HIGH)
        );
    }

    @Test
    public void sandboxedVertexUndoShouldPushDeletionStatus() {
        Vertex vertex = newVertexOnWorkspace();
        List<ClientApiWorkspaceDiff.Item> diffs = getDiffsFromWorkspace();
        undoWorkspaceDiffs(diffs);
        verify(workQueueRepository).pushVertexDeletion(eq(vertex), any(Long.class), eq(Priority.HIGH));
    }

    @Test
    public void sandboxedPublicVertexDeletionShouldPushHiddenStatus() {
        workspaceHelper.deleteVertex(
                entity1Vertex,
                workspace.getWorkspaceId(),
                true,
                Priority.HIGH,
                workspaceAuthorizations,
                user1
        );
        verify(workQueueRepository).pushVertexHidden(eq(entity1Vertex), any(Long.class), eq(Priority.HIGH));
    }

    @Test
    public void sandboxedPublicVertexDeletionUndoShouldPushUnhiddenStatus() {
        workspaceHelper.deleteVertex(
                entity1Vertex,
                workspace.getWorkspaceId(),
                true,
                Priority.HIGH,
                workspaceAuthorizations,
                user1
        );
        verify(workQueueRepository).pushVertexHidden(eq(entity1Vertex), any(Long.class), eq(Priority.HIGH));
        List<ClientApiWorkspaceDiff.Item> diffs = getDiffsFromWorkspace();
        undoWorkspaceDiffs(diffs);
        verify(workQueueRepository).pushVertexUnhidden(eq(entity1Vertex), eq(Priority.HIGH));
    }

    @Test
    public void publishedPublicVertexDeletionShouldPushDeletionStatus() {
        workspaceHelper.deleteVertex(
                entity1Vertex,
                workspace.getWorkspaceId(),
                true,
                Priority.HIGH,
                workspaceAuthorizations,
                user1
        );
        verify(workQueueRepository).pushVertexHidden(eq(entity1Vertex), any(Long.class), eq(Priority.HIGH));
        publishAllWorkspaceDiffs();
        verify(workQueueRepository).pushPublishedVertexDeletion(eq(entity1Vertex), any(Long.class), eq(Priority.HIGH));
    }

    @Test
    public void sandboxedEdgeUndoShouldPushDeletionStatus() {
        Edge edge = newEdgeOnWorkspace();
        List<ClientApiWorkspaceDiff.Item> diffs = getDiffsFromWorkspace();
        undoWorkspaceDiffs(diffs);
        verify(workQueueRepository).pushEdgeDeletion(eq(edge), any(Long.class), eq(Priority.HIGH));
    }

    @Test
    public void sandboxedPublicEdgeDeletionShouldPushHiddenStatus() {
        Edge edge = newEdgeOnWorkspace();
        publishAllWorkspaceDiffs();

        Vertex v1 = graph.getVertex("v1", workspaceAuthorizations);
        Vertex v2 = graph.getVertex("v2", workspaceAuthorizations);

        workspaceHelper.deleteEdge(
                workspace.getWorkspaceId(),
                edge,
                v1,
                v2,
                true,
                Priority.HIGH,
                workspaceAuthorizations,
                user1
        );
        verify(workQueueRepository).pushEdgeHidden(eq(edge), any(Long.class), eq(Priority.HIGH));
    }

    @Test
    public void sandboxedPublicEdgeDeletionUndoShouldPushUnhiddenStatus() {
        Edge edge = newEdgeOnWorkspace();
        publishAllWorkspaceDiffs();

        Vertex v1 = graph.getVertex("v1", workspaceAuthorizations);
        Vertex v2 = graph.getVertex("v2", workspaceAuthorizations);

        workspaceHelper.deleteEdge(
                workspace.getWorkspaceId(),
                edge,
                v1,
                v2,
                true,
                Priority.HIGH,
                workspaceAuthorizations,
                user1
        );
        List<ClientApiWorkspaceDiff.Item> diffs = getDiffsFromWorkspace();
        undoWorkspaceDiffs(diffs);
        verify(workQueueRepository).pushEdgeUnhidden(eq(edge), eq(Priority.HIGH));
    }

    @Test
    public void publishedPublicEdgeDeletionShouldPushDeletionStatus() {
        Edge edge = newEdgeOnWorkspace();
        publishAllWorkspaceDiffs();

        Vertex v1 = graph.getVertex("v1", workspaceAuthorizations);
        Vertex v2 = graph.getVertex("v2", workspaceAuthorizations);

        workspaceHelper.deleteEdge(
                workspace.getWorkspaceId(),
                edge,
                v1,
                v2,
                true,
                Priority.HIGH,
                workspaceAuthorizations,
                user1
        );
        publishAllWorkspaceDiffs();
        verify(workQueueRepository).pushPublishedEdgeDeletion(eq(edge), any(Long.class), eq(Priority.HIGH));
    }

    private Edge newEdgeOnWorkspace() {
        when(ontologyRepository.getPropertyByIRI(VisalloProperties.MODIFIED_BY.getPropertyName()))
                .thenReturn(new InMemoryOntologyProperty());
        Vertex v1 = graph.prepareVertex("v1", initialVisibility).save(workspaceAuthorizations);
        Vertex v2 = graph.prepareVertex("v2", initialVisibility).save(workspaceAuthorizations);
        EdgeBuilder edgeBuilder = graph.prepareEdge("edge1", v1, v2, "label1", initialWorkspaceViz);
        VisalloProperties.VISIBILITY_JSON.setProperty(edgeBuilder, initialVisibilityJson, new Visibility(""));
        VisalloProperties.MODIFIED_BY.setProperty(edgeBuilder, "testUser", initialWorkspaceViz);
        Edge edge = edgeBuilder.save(workspaceAuthorizations);
        graph.flush();

        workspaceRepository.updateEntityOnWorkspace(workspace, v1.getId(), user1);
        workspaceRepository.updateEntityOnWorkspace(workspace, v2.getId(), user1);
        return edge;
    }

    private Vertex newVertexOnWorkspace() {
        when(injector.getInstance(OntologyRepository.class)).thenReturn(ontologyRepository);
        when(ontologyRepository.getPropertyByIRI(VisalloProperties.CONCEPT_TYPE.getPropertyName()))
                .thenReturn(new InMemoryOntologyProperty());
        InMemoryOntologyProperty titleOntologyProp = new InMemoryOntologyProperty();
        titleOntologyProp.setUserVisible(true);
        when(ontologyRepository.getPropertyByIRI(VisalloProperties.TITLE.getPropertyName())).thenReturn(
                titleOntologyProp);

        VertexBuilder vertexBuilder = graph.prepareVertex(initialWorkspaceViz);
        VisalloProperties.VISIBILITY_JSON.setProperty(vertexBuilder, initialVisibilityJson, new Visibility(""));

        // Note that GraphRepository does not create new Metadata for each property. Doing that here is needed due
        // to the use of InMemoryGraph.
        Metadata propertyMetadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(propertyMetadata, initialVisibilityJson,
                                                               DEFAULT_VISIBILITY
        );
        VisalloProperties.CONCEPT_TYPE.setProperty(
                vertexBuilder,
                VERTEX_CONCEPT_TYPE,
                propertyMetadata,
                initialWorkspaceViz
        );

        propertyMetadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(propertyMetadata, initialVisibilityJson,
                                                               DEFAULT_VISIBILITY
        );
        VisalloProperties.TITLE.addPropertyValue(
                vertexBuilder,
                "",
                VERTEX_TITLE,
                propertyMetadata,
                initialWorkspaceViz
        );

        Vertex vertex = vertexBuilder.save(workspaceAuthorizations);
        graph.flush();

        workspaceRepository.updateEntityOnWorkspace(workspace, vertex.getId(), user1);
        graph.flush();
        return vertex;
    }

    private void undoWorkspaceDiffs(List<ClientApiWorkspaceDiff.Item> diffs) {
        List<ClientApiUndoItem> undoItems = new ArrayList<>(diffs.size());
        for (ClientApiWorkspaceDiff.Item diff : diffs) {
            if (diff instanceof ClientApiWorkspaceDiff.VertexItem) {
                ClientApiVertexUndoItem item = new ClientApiVertexUndoItem();
                item.setVertexId(((ClientApiWorkspaceDiff.VertexItem) diff).getVertexId());
                undoItems.add(item);
            } else if (diff instanceof ClientApiWorkspaceDiff.EdgeItem) {
                ClientApiRelationshipUndoItem item = new ClientApiRelationshipUndoItem();
                item.setEdgeId(((ClientApiWorkspaceDiff.EdgeItem) diff).getEdgeId());
                undoItems.add(item);
            } else if (diff instanceof ClientApiWorkspaceDiff.PropertyItem) {
                undoItems.add(createPropertyUndoItem((ClientApiWorkspaceDiff.PropertyItem) diff));
            }
        }
        ClientApiWorkspaceUndoResponse response = new ClientApiWorkspaceUndoResponse();
        workspaceUndoHelper.undo(undoItems, response, workspace.getWorkspaceId(), user1, workspaceAuthorizations);
        assertTrue(response.isSuccess());
    }

    private void undoAllWorkspaceDiffs() {
        List<ClientApiWorkspaceDiff.Item> allDiffs = getDiffsFromWorkspace();

        assertTrue("no diffs were found to undo", allDiffs.size() > 0);
        undoWorkspaceDiffs(allDiffs);
        assertNoDiffs();
    }

    private void publishAllWorkspaceDiffs() {
        List<VertexItem> vertexDiffs = getDiffsFromWorkspace(VertexItem.class);
        List<EdgeItem> edgeDiffs = getDiffsFromWorkspace(EdgeItem.class);
        List<PropertyItem> propertyDiffs = getDiffsFromWorkspace(PropertyItem.class);

        assertTrue("no diffs were found to publish", vertexDiffs.size() + propertyDiffs.size() + edgeDiffs.size() > 0);

        List<ClientApiPublishItem> vertexItems = new ArrayList<>();
        for (VertexItem diff : vertexDiffs) {
            ClientApiVertexPublishItem publishItem = new ClientApiVertexPublishItem();
            publishItem.setVertexId(diff.getVertexId());
            publishItem.setAction(diff.isDeleted() ? Action.DELETE : Action.ADD_OR_UPDATE);
            vertexItems.add(publishItem);
        }

        List<ClientApiPublishItem> propertyItems = new ArrayList<>();
        for (PropertyItem diff : propertyDiffs) {
            ClientApiPropertyPublishItem publishItem = new ClientApiPropertyPublishItem();
            publishItem.setElementId(diff.getElementId());
            publishItem.setKey(diff.getKey());
            publishItem.setName(diff.getName());
            publishItem.setVisibilityString(diff.getVisibilityString());
            publishItem.setAction(diff.isDeleted() ? Action.DELETE : Action.ADD_OR_UPDATE);
            propertyItems.add(publishItem);
        }

        List<ClientApiPublishItem> edgeItems = new ArrayList<>();
        for (EdgeItem diff : edgeDiffs) {
            ClientApiRelationshipPublishItem publishItem = new ClientApiRelationshipPublishItem();
            publishItem.setEdgeId(diff.getEdgeId());
            publishItem.setAction(diff.isDeleted() ? Action.DELETE : Action.ADD_OR_UPDATE);
            edgeItems.add(publishItem);
        }

        ClientApiPublishItem[] allPublishItems = Lists.newArrayList(Iterables.concat(
                vertexItems,
                propertyItems,
                edgeItems
        ))
                .toArray(new ClientApiPublishItem[vertexDiffs.size() + propertyDiffs.size() + edgeDiffs.size()]);

        ClientApiWorkspacePublishResponse response =
                workspaceRepository.publish(allPublishItems, user1, workspace.getWorkspaceId(), workspaceAuthorizations);
        assertTrue(response.isSuccess());
        assertNoDiffs();
    }

    private List<ClientApiWorkspaceDiff.Item> getDiffsFromWorkspace() {
        return workspaceRepository.getDiff(workspace, user1, userContext).getDiffs();
    }

    private <T extends ClientApiWorkspaceDiff.Item> List<T> getDiffsFromWorkspace(Class<T> itemType) {
        return getDiffsFromWorkspace()
                .stream()
                .filter(diff -> itemType.isAssignableFrom(diff.getClass()))
                .map(itemType::cast)
                .collect(Collectors.toList());
    }

    private void undoPropertyDiffs(List<PropertyItem> diffs) {
        List<ClientApiUndoItem> undoItems =
                diffs.stream()
                        .map(this::createPropertyUndoItem)
                        .collect(Collectors.toList());
        ClientApiWorkspaceUndoResponse response = new ClientApiWorkspaceUndoResponse();
        workspaceUndoHelper.undo(undoItems, response, workspace.getWorkspaceId(), user1, workspaceAuthorizations);
        assertTrue(response.isSuccess());
        assertNoDiffs();
    }

    private ClientApiPropertyUndoItem createPropertyUndoItem(PropertyItem diff) {
        ElementType elementType = ElementType.valueOf(diff.getElementType().toUpperCase());
        String elementId = diff.getElementId();
        ClientApiPropertyUndoItem item = new ClientApiPropertyUndoItem();
        if (elementType == ElementType.VERTEX) {
            item.setVertexId(elementId);
        } else if (elementType == ElementType.EDGE) {
            item.setEdgeId(elementId);
        }
        item.setElementId(elementId);
        item.setName(diff.getName());
        item.setKey(diff.getKey());
        item.setVisibilityString(diff.getVisibilityString());
        return item;
    }

    private void markPublicPropertyHiddenOnWorkspace() {
        entity1Vertex.markPropertyHidden("key1", "prop1", initialVisibility, new Visibility(workspace.getWorkspaceId()),
                                         workspaceAuthorizations
        );
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
        entity1Vertex = graph.getVertex(entity1Vertex.getId(), workspaceAuthorizations);
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
        entity1Vertex = graph.getVertex(entity1Vertex.getId(), workspaceAuthorizations);
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
        entity1Vertex = graph.getVertex(entity1Vertex.getId(), workspaceAuthorizations);
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
                                         workspaceAuthorizations
        );
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

    private void assertNoDiffs() {
        assertEquals(0, workspaceRepository.getDiff(workspace, user1, userContext).getDiffs().size());
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
