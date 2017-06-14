package org.visallo.vertexium.model.workspace;

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
import org.visallo.core.model.graph.VisibilityAndElementMutation;
import org.visallo.core.model.ontology.*;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserPropertyAuthorizationRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceUndoHelper;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.core.util.VisalloInMemoryTestBase;
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
@RunWith(Parameterized.class)
public class VertexiumWorkspaceSandboxingTest extends VisalloInMemoryTestBase {
    private static final Locale LOCALE = Locale.US;
    private static final String TIME_ZONE = Calendar.getInstance().getTimeZone().getID();
    private static final String WORKSPACE_ID = "testWorkspaceId";
    private static final String SECRET_VISIBILITY_SOURCE = "secret";
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


    private User user;
    private Visibility secretVisibility;
    private Vertex entity1Vertex;
    private WorkspaceHelper workspaceHelper;
    private WorkspaceUndoHelper workspaceUndoHelper;

    @Parameterized.Parameters
    public static Iterable<Object[]> initialVisibilitySources() {
        return Arrays.asList(new Object[][]{
                {""}, {OTHER_VISIBILITY_SOURCE}
        });
    }

    public VertexiumWorkspaceSandboxingTest(String initialVisibilitySource) {
        this.initialVisibilitySource = initialVisibilitySource;
    }

    @Before
    public void before() {
        super.before();

        user = getUserRepository().findOrAddUser("junit", "Junit", "junit@visallo.com", "password");

        initialVisibility = getVisibilityTranslator().toVisibility(new VisibilityJson(initialVisibilitySource)).getVisibility();
        secretVisibility = getVisibilityTranslator().toVisibility(new VisibilityJson(SECRET_VISIBILITY_SOURCE)).getVisibility();

        User systemUser = getUserRepository().getSystemUser();
        Authorizations systemUserAuth = getAuthorizationRepository().getGraphAuthorizations(systemUser);
        Visibility defaultVisibility = getVisibilityTranslator().getDefaultVisibility();
        entity1Vertex = getGraph().prepareVertex("entity1aId", initialVisibility)
                .addPropertyValue("key1", "prop1", "value1", new Metadata(), initialVisibility)
                .addPropertyValue("key9", "prop9", "value9", new Metadata(), initialVisibility)
                .save(systemUserAuth);

        Concept thing = getOntologyRepository().getEntityConcept(systemUser, null);
        Relationship hasEntityRel = getOntologyRepository().getOrCreateRelationshipType(null, Collections.singleton(thing), Collections.singleton(thing), "has-entity-iri", true, systemUser, null);
        hasEntityRel.addIntent("entityHasImage", systemUserAuth);

        getOntologyRepository().getOrCreateConcept(thing, VERTEX_CONCEPT_TYPE, VERTEX_CONCEPT_TYPE, null, systemUser, null);

        OntologyPropertyDefinition propertyDefinition = new OntologyPropertyDefinition(
                Collections.singletonList(thing),
                VisalloProperties.VISIBILITY_JSON.getPropertyName(),
                "Visibility JSON",
                PropertyType.STRING
        );
        propertyDefinition.setTextIndexHints(TextIndexHint.NONE);
        getOntologyRepository().getOrCreateProperty(propertyDefinition, systemUser, null);

        propertyDefinition = new OntologyPropertyDefinition(
                Collections.singletonList(thing),
                VisalloProperties.CONCEPT_TYPE.getPropertyName(),
                "Concept Type",
                PropertyType.STRING
        );
        propertyDefinition.setTextIndexHints(Collections.singleton(TextIndexHint.EXACT_MATCH));
        getOntologyRepository().getOrCreateProperty(propertyDefinition, systemUser, null);

        propertyDefinition = new OntologyPropertyDefinition(
                Collections.singletonList(thing),
                VisalloProperties.TITLE.getPropertyName(),
                "Title",
                PropertyType.STRING
        );
        propertyDefinition.setTextIndexHints(Collections.singleton(TextIndexHint.EXACT_MATCH));
        propertyDefinition.setUserVisible(true);
        getOntologyRepository().getOrCreateProperty(propertyDefinition, systemUser, null);

        propertyDefinition = new OntologyPropertyDefinition(
                Collections.singletonList(thing),
                VisalloProperties.MODIFIED_BY.getPropertyName(),
                "Visibility JSON",
                PropertyType.DATE
        );
        getOntologyRepository().getOrCreateProperty(propertyDefinition, systemUser, null);

        propertyDefinition = new OntologyPropertyDefinition(Collections.singletonList(thing), "prop1", "Prop 1", PropertyType.STRING);
        propertyDefinition.setUserVisible(true);
        propertyDefinition.setTextIndexHints(Collections.singleton(TextIndexHint.EXACT_MATCH));
        getOntologyRepository().getOrCreateProperty(propertyDefinition, systemUser, null);

        getOntologyRepository().getOrCreateRelationshipType(null, Collections.singleton(thing), Collections.singleton(thing), "label1", true, systemUser, null);

        UserPropertyAuthorizationRepository authorizationRepository = (UserPropertyAuthorizationRepository) getAuthorizationRepository();
        authorizationRepository.addAuthorization(user, SECRET_VISIBILITY_SOURCE, systemUser);
        authorizationRepository.addAuthorization(user, OTHER_VISIBILITY_SOURCE, systemUser);

        workspace = getWorkspaceRepository().add(WORKSPACE_ID, "testWorkspaceTitle", user);
        workspaceAuthorizations = new InMemoryAuthorizations(
                WORKSPACE_ID,
                SECRET_VISIBILITY_SOURCE,
                OTHER_VISIBILITY_SOURCE
        );

        // important: reload vertex with all authorizations
        entity1Vertex = getGraph().getVertex(entity1Vertex.getId(), workspaceAuthorizations);

        initialVisibilityJson = new VisibilityJson(initialVisibilitySource);
        initialVisibilityJson.addWorkspace(WORKSPACE_ID);
        initialWorkspaceViz = getVisibilityTranslator().toVisibilityNoSuperUser(initialVisibilityJson);
        initialMetadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(
                initialMetadata,
                initialVisibilityJson,
                getVisibilityTranslator().getDefaultVisibility()
        );

        VisibilityJson secretVisibilityJson = new VisibilityJson(SECRET_VISIBILITY_SOURCE);
        secretVisibilityJson.addWorkspace(WORKSPACE_ID);
        secretWorkspaceViz = getVisibilityTranslator().toVisibilityNoSuperUser(secretVisibilityJson);
        secretMetadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(
                secretMetadata,
                secretVisibilityJson,
                getVisibilityTranslator().getDefaultVisibility()
        );

        getWorkspaceRepository().updateEntityOnWorkspace(workspace, entity1Vertex.getId(), user);

        userContext = new FormulaEvaluator.UserContext(LOCALE, null, TIME_ZONE, WORKSPACE_ID);

        workspaceHelper = new WorkspaceHelper(
                getTermMentionRepository(),
                getWorkQueueRepository(),
                getGraph(),
                getOntologyRepository(),
                getWorkspaceRepository(),
                getPrivilegeRepository(),
                authorizationRepository
        );

        workspaceUndoHelper = new WorkspaceUndoHelper(
                getGraph(),
                workspaceHelper,
                getWorkQueueRepository()
        );
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

        vertex = getGraph().getVertex(vertex.getId(), workspaceAuthorizations);
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
        vertex = getGraph().getVertex(vertex.getId(), workspaceAuthorizations);

        // delete the vertex
        SandboxStatus sandboxStatus = SandboxStatusUtil.getSandboxStatus(vertex, WORKSPACE_ID);
        assertEquals(SandboxStatus.PUBLIC, sandboxStatus);
        workspaceHelper.deleteVertex(
                vertex, WORKSPACE_ID, true, Priority.HIGH, workspaceAuthorizations, user);

        List<VertexItem> vertexDiffs = getDiffsFromWorkspace(VertexItem.class);

        assertEquals(1, vertexDiffs.size());
        VertexItem vertexDiff = vertexDiffs.get(0);
        assertEquals(vertex.getId(), vertexDiff.getVertexId());
        assertEquals(VERTEX_CONCEPT_TYPE, vertexDiff.getConceptType());
        assertEquals(VERTEX_TITLE, vertexDiff.getTitle());
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

        assertNull(getGraph().getVertex(vertex.getId(), workspaceAuthorizations));
    }

    @Test
    public void undoDeleteVertexSucceeds() {
        Vertex vertex = newVertexOnWorkspace();
        String vertexId = vertex.getId();

        // publish the new vertex
        publishAllWorkspaceDiffs();
        vertex = getGraph().getVertex(vertexId, workspaceAuthorizations);

        // delete the vertex
        SandboxStatus sandboxStatus = SandboxStatusUtil.getSandboxStatus(vertex, WORKSPACE_ID);
        assertEquals(SandboxStatus.PUBLIC, sandboxStatus);
        workspaceHelper.deleteVertex(
                vertex, WORKSPACE_ID, true, Priority.HIGH, workspaceAuthorizations, user);
        assertNull(getGraph().getVertex(vertexId, workspaceAuthorizations));

        assertEquals(1, getDiffsFromWorkspace(VertexItem.class).size());
        assertEquals(0, getDiffsFromWorkspace(PropertyItem.class).size());
        assertEquals(0, getDiffsFromWorkspace(EdgeItem.class).size());

        // undo the vertex deletion
        undoAllWorkspaceDiffs();

        assertEquals(0, getDiffsFromWorkspace(VertexItem.class).size());
        assertEquals(0, getDiffsFromWorkspace(PropertyItem.class).size());
        assertEquals(0, getDiffsFromWorkspace(EdgeItem.class).size());

        vertex = getGraph().getVertex(vertexId, workspaceAuthorizations);
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
                                                               getVisibilityTranslator().getDefaultVisibility()
        );

        ElementBuilder<Vertex> vertexBuilder = getGraph().prepareVertex("v1", initialVisibility)
                .addPropertyValue("k1", "p1", "v1", propertyMetadata, initialVisibility)
                .addPropertyValue("k2", "p2", "v2", propertyMetadata, initialVisibility);
        VisalloProperties.VISIBILITY_JSON.setProperty(
                vertexBuilder, initialVisibilityJson, propertyMetadata, new Visibility(""));
        Vertex v1 = vertexBuilder.save(workspaceAuthorizations);
        getGraph().flush();

        getWorkspaceRepository().updateEntityOnWorkspace(workspace, v1.getId(), user);
        getGraph().flush();

        String vertexId = v1.getId();
        assertNotNull(getGraph().getVertex(vertexId, workspaceAuthorizations));
        List<ClientApiWorkspaceDiff.Item> diffs = getDiffsFromWorkspace();
        assertEquals(4, diffs.size());
        undoWorkspaceDiffs(diffs);
        assertNull(getGraph().getVertex(vertexId, workspaceAuthorizations));

        vertexBuilder = getGraph().prepareVertex("v1", initialVisibility)
                .addPropertyValue("k3", "p1", "v1", propertyMetadata, initialVisibility)
                .addPropertyValue("k4", "p2", "v2", propertyMetadata, initialVisibility);
        VisalloProperties.VISIBILITY_JSON.setProperty(
                vertexBuilder, initialVisibilityJson, propertyMetadata, new Visibility(""));
        vertexBuilder.save(workspaceAuthorizations);
        getGraph().flush();

        getWorkspaceRepository().updateEntityOnWorkspace(workspace, v1.getId(), user);
        getGraph().flush();

        assertNotNull(getGraph().getVertex(vertexId, workspaceAuthorizations));
        diffs = getDiffsFromWorkspace();
        assertEquals(4, diffs.size());
        undoWorkspaceDiffs(diffs);
        assertNull(getGraph().getVertex(vertexId, workspaceAuthorizations));
    }

    @Test
    public void sandboxedPublicPropertyDeletionShouldPushHiddenStatus() {
        changePublicPropertyValueOnWorkspace();
        getGraph().flush();
        publishAllWorkspaceDiffs();

        entity1Vertex = getGraph().getVertex(entity1Vertex.getId(), workspaceAuthorizations);
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

        List<byte[]> workQueueItems = getWorkQueueItems(getWorkQueueNames().getGraphPropertyQueueName());
        // TODO: Verify work queue items
    }

    @Test
    public void sandboxedPropertyUndoShouldPushDeletionStatus() {
        changePublicPropertyValueOnWorkspace();
        List<ClientApiWorkspaceDiff.PropertyItem> diffs = getDiffsFromWorkspace(ClientApiWorkspaceDiff.PropertyItem.class);
        undoPropertyDiffs(diffs);

        List<byte[]> workQueueItems = getWorkQueueItems(getWorkQueueNames().getGraphPropertyQueueName());
        // TODO: Verify work queue items
    }

    @Test
    public void sandboxedPublicPropertyDeletionUndoShouldPushUnhiddenStatus() {
        markPublicPropertyHiddenOnWorkspace();
        List<ClientApiWorkspaceDiff.PropertyItem> diffs = getDiffsFromWorkspace(ClientApiWorkspaceDiff.PropertyItem.class);
        undoPropertyDiffs(diffs);

        List<byte[]> workQueueItems = getWorkQueueItems(getWorkQueueNames().getGraphPropertyQueueName());
        // TODO: Verify work queue items
    }

    @Test
    public void publishedPublicPropertyDeletionShouldPushDeletionStatus() {
        markPublicPropertyHiddenOnWorkspace();
        publishAllWorkspaceDiffs();

        List<byte[]> workQueueItems = getWorkQueueItems(getWorkQueueNames().getGraphPropertyQueueName());
        // TODO: Verify work queue items
    }

    @Test
    public void sandboxedVertexUndoShouldPushDeletionStatus() {
        Vertex vertex = newVertexOnWorkspace();
        List<ClientApiWorkspaceDiff.Item> diffs = getDiffsFromWorkspace();
        undoWorkspaceDiffs(diffs);

        List<byte[]> workQueueItems = getWorkQueueItems(getWorkQueueNames().getGraphPropertyQueueName());
        // TODO: Verify work queue items
    }

    @Test
    public void sandboxedPublicVertexDeletionShouldPushHiddenStatus() {
        workspaceHelper.deleteVertex(
                entity1Vertex,
                WORKSPACE_ID,
                true,
                Priority.HIGH,
                workspaceAuthorizations,
                user
        );

        List<byte[]> workQueueItems = getWorkQueueItems(getWorkQueueNames().getGraphPropertyQueueName());
        // TODO: Verify work queue items
    }

    @Test
    public void sandboxedPublicVertexDeletionUndoShouldPushUnhiddenStatus() {
        workspaceHelper.deleteVertex(
                entity1Vertex,
                WORKSPACE_ID,
                true,
                Priority.HIGH,
                workspaceAuthorizations,
                user
        );
        List<byte[]> workQueueItems = getWorkQueueItems(getWorkQueueNames().getGraphPropertyQueueName());
        // TODO: Verify work queue items

        List<ClientApiWorkspaceDiff.Item> diffs = getDiffsFromWorkspace();
        undoWorkspaceDiffs(diffs);
        // TODO: Verify work queue items
    }

    @Test
    public void publishedPublicVertexDeletionShouldPushDeletionStatus() {
        workspaceHelper.deleteVertex(
                entity1Vertex,
                WORKSPACE_ID,
                true,
                Priority.HIGH,
                workspaceAuthorizations,
                user
        );
        List<byte[]> workQueueItems = getWorkQueueItems(getWorkQueueNames().getGraphPropertyQueueName());
        // TODO: Verify work queue items

        publishAllWorkspaceDiffs();
        // TODO: Verify work queue items
    }

    @Test
    public void sandboxedEdgeUndoShouldPushDeletionStatus() {
        Edge edge = newEdgeOnWorkspace();
        List<ClientApiWorkspaceDiff.Item> diffs = getDiffsFromWorkspace();
        undoWorkspaceDiffs(diffs);
        List<byte[]> workQueueItems = getWorkQueueItems(getWorkQueueNames().getGraphPropertyQueueName());
        // TODO: Verify work queue items
    }

    @Test
    public void sandboxedPublicEdgeDeletionShouldPushHiddenStatus() {
        Edge edge = newEdgeOnWorkspace();
        publishAllWorkspaceDiffs();

        Vertex v1 = getGraph().getVertex("v1", workspaceAuthorizations);
        Vertex v2 = getGraph().getVertex("v2", workspaceAuthorizations);

        workspaceHelper.deleteEdge(
                WORKSPACE_ID,
                edge,
                v1,
                v2,
                true,
                Priority.HIGH,
                workspaceAuthorizations,
                user
        );

        List<byte[]> workQueueItems = getWorkQueueItems(getWorkQueueNames().getGraphPropertyQueueName());
        // TODO: Verify work queue items
    }

    @Test
    public void sandboxedPublicEdgeDeletionUndoShouldPushUnhiddenStatus() {
        Edge edge = newEdgeOnWorkspace();
        publishAllWorkspaceDiffs();

        Vertex v1 = getGraph().getVertex("v1", workspaceAuthorizations);
        Vertex v2 = getGraph().getVertex("v2", workspaceAuthorizations);

        workspaceHelper.deleteEdge(
                WORKSPACE_ID,
                edge,
                v1,
                v2,
                true,
                Priority.HIGH,
                workspaceAuthorizations,
                user
        );
        List<ClientApiWorkspaceDiff.Item> diffs = getDiffsFromWorkspace();
        undoWorkspaceDiffs(diffs);

        List<byte[]> workQueueItems = getWorkQueueItems(getWorkQueueNames().getGraphPropertyQueueName());
        // TODO: Verify work queue items
    }

    @Test
    public void publishedPublicEdgeDeletionShouldPushDeletionStatus() {
        Edge edge = newEdgeOnWorkspace();
        publishAllWorkspaceDiffs();

        Vertex v1 = getGraph().getVertex("v1", workspaceAuthorizations);
        Vertex v2 = getGraph().getVertex("v2", workspaceAuthorizations);

        workspaceHelper.deleteEdge(
                WORKSPACE_ID,
                edge,
                v1,
                v2,
                true,
                Priority.HIGH,
                workspaceAuthorizations,
                user
        );
        publishAllWorkspaceDiffs();

        List<byte[]> workQueueItems = getWorkQueueItems(getWorkQueueNames().getGraphPropertyQueueName());
        // TODO: Verify work queue items
    }

    private Edge newEdgeOnWorkspace() {
        Vertex v1 = getGraph().prepareVertex("v1", initialVisibility).save(workspaceAuthorizations);
        Vertex v2 = getGraph().prepareVertex("v2", initialVisibility).save(workspaceAuthorizations);
        EdgeBuilder edgeBuilder = getGraph().prepareEdge("edge1", v1, v2, "label1", initialWorkspaceViz);
        VisalloProperties.VISIBILITY_JSON.setProperty(edgeBuilder, initialVisibilityJson, new Visibility(""));
        VisalloProperties.MODIFIED_BY.setProperty(edgeBuilder, "testUser", initialWorkspaceViz);
        Edge edge = edgeBuilder.save(workspaceAuthorizations);
        getGraph().flush();

        getWorkspaceRepository().updateEntityOnWorkspace(workspace, v1.getId(), user);
        getWorkspaceRepository().updateEntityOnWorkspace(workspace, v2.getId(), user);
        return edge;
    }

    private Vertex newVertexOnWorkspace() {
        VertexBuilder vertexBuilder = getGraph().prepareVertex(initialWorkspaceViz);
        VisalloProperties.VISIBILITY_JSON.setProperty(vertexBuilder, initialVisibilityJson, new Visibility(""));

        // Note that GraphRepository does not create new Metadata for each property. Doing that here is needed due
        // to the use of InMemoryGraph.
        Metadata propertyMetadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(propertyMetadata, initialVisibilityJson,
                                                               getVisibilityTranslator().getDefaultVisibility()
        );
        VisalloProperties.CONCEPT_TYPE.setProperty(
                vertexBuilder,
                VERTEX_CONCEPT_TYPE,
                propertyMetadata,
                initialWorkspaceViz
        );

        propertyMetadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(propertyMetadata, initialVisibilityJson,
                getVisibilityTranslator().getDefaultVisibility()
        );
        VisalloProperties.TITLE.addPropertyValue(
                vertexBuilder,
                "",
                VERTEX_TITLE,
                propertyMetadata,
                initialWorkspaceViz
        );

        Vertex vertex = vertexBuilder.save(workspaceAuthorizations);
        getGraph().flush();

        getWorkspaceRepository().updateEntityOnWorkspace(workspace, vertex.getId(), user);
        getGraph().flush();
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
        workspaceUndoHelper.undo(undoItems, response, WORKSPACE_ID, user, workspaceAuthorizations);
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
                getWorkspaceRepository().publish(allPublishItems, user, WORKSPACE_ID, workspaceAuthorizations);
        assertTrue(response.isSuccess());
        assertNoDiffs();
    }

    private List<ClientApiWorkspaceDiff.Item> getDiffsFromWorkspace() {
        return getWorkspaceRepository().getDiff(workspace, user, userContext).getDiffs();
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
        workspaceUndoHelper.undo(undoItems, response, WORKSPACE_ID, user, workspaceAuthorizations);
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
        OntologyProperty ontologyProperty = getOntologyRepository().getRequiredPropertyByIRI("prop1");
        workspaceHelper.deleteProperties(
                entity1Vertex,
                "key1",
                "prop1",
                ontologyProperty,
                WORKSPACE_ID,
                workspaceAuthorizations,
                user);
        getGraph().flush();
    }

    private void changePublicPropertyValueOnWorkspace() {
        VisibilityAndElementMutation<Vertex> visibilityAndMutation = getGraphRepository().setProperty(
                entity1Vertex,
                "prop1",
                "key1",
                "value1a",
                initialMetadata,
                initialVisibilitySource,
                initialVisibilitySource,
                WORKSPACE_ID,
                "",
                null,
                user,
                workspaceAuthorizations
        );
        visibilityAndMutation.elementMutation.save(workspaceAuthorizations);
        getGraph().flush();
    }

    private void assertChangedPropertyValueDiff(PropertyItem diff) {
        assertEquals("key1", diff.getKey());
        assertEquals("prop1", diff.getName());
        assertEquals("value1", diff.getOldData().get("value").asText());
        assertEquals("value1a", diff.getNewData().get("value").asText());
        assertVisibilityEquals(getVisibilityTranslator().toVisibility(initialVisibilityJson).toString(), diff.getVisibilityString());
        assertEquals(SandboxStatus.PUBLIC_CHANGED, diff.getSandboxStatus());
        assertFalse(diff.isDeleted());
    }

    private void assertChangedPropertyValuePublished() {
        entity1Vertex = getGraph().getVertex(entity1Vertex.getId(), workspaceAuthorizations);
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
        entity1Vertex = getGraph().getVertex(entity1Vertex.getId(), workspaceAuthorizations);
        Property property = entity1Vertex.getProperty("key1", "prop1");
        assertEquals("value1", property.getValue());
        assertVisibilityOnProperty(secretVisibility, property);
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
        entity1Vertex = getGraph().getVertex(entity1Vertex.getId(), workspaceAuthorizations);
        Property property = entity1Vertex.getProperty("key1", "prop1");
        assertEquals("value1a", property.getValue());
        assertVisibilityOnProperty(secretVisibility, property);
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
        entity1Vertex.markPropertyHidden("key9", "prop9", initialVisibility, new Visibility(WORKSPACE_ID),
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
        assertEquals(0, getWorkspaceRepository().getDiff(workspace, user, userContext).getDiffs().size());
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
        return s.replaceAll("[\\(\\)]", "");
    }

    private static String extractVisibilitySource(String visibilityString) {
        return stripParenthesis(visibilityString)
                .replaceAll("[\\&\\|]", "")
                .replace(VisalloVisibility.SUPER_USER_VISIBILITY_STRING, "");
    }
}
