package org.visallo.core.model.workspace;

import com.google.common.collect.Sets;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.vertexium.*;
import org.visallo.core.model.graph.VisibilityAndElementMutation;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyPropertyDefinition;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.user.UserPropertyPrivilegeRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workspace.product.Product;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.core.util.VisalloInMemoryTestBase;
import org.visallo.web.clientapi.model.*;

import java.util.*;

import static org.junit.Assert.*;

public abstract class WorkspaceRepositoryTestBase extends VisalloInMemoryTestBase {
    private static final String JUNIT_CONCEPT_TYPE = "junit-concept-iri";
    private static final String JUNIT_EDGE_LABEL = "junit-edge-iri";
    private static final String JUNIT_PROPERTY_NAME = "junit-property-iri";

    private WorkspaceHelper workspaceHelper;

    private User user;
    private Workspace workspace;
    private Authorizations workspaceAuthorizations;
    private Concept thingConcept;

    @Before
    public void before() {
        super.before();
        user = getUserRepository().findOrAddUser("junit", "Junit", "junit@visallo.com", "password");

        User systemUser = getUserRepository().getSystemUser();
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(systemUser);
        thingConcept = getOntologyRepository().getEntityConcept(systemUser, null);

        List<Concept> things = Collections.singletonList(thingConcept);
        Relationship hasEntityRel = getOntologyRepository().getOrCreateRelationshipType(null, things, things, "has-entity-iri", true, systemUser, null);
        hasEntityRel.addIntent("entityHasImage", authorizations);

        getOntologyRepository().getOrCreateConcept(thingConcept, JUNIT_CONCEPT_TYPE, "Junit Concept", null, systemUser, null);
        getOntologyRepository().getOrCreateRelationshipType(null, things, things, JUNIT_EDGE_LABEL, true, systemUser, null);
        OntologyPropertyDefinition propertyDefinition = new OntologyPropertyDefinition(things, JUNIT_PROPERTY_NAME, "Junit Property", PropertyType.STRING);
        propertyDefinition.setTextIndexHints(Collections.singleton(TextIndexHint.EXACT_MATCH));
        propertyDefinition.setUserVisible(true);
        getOntologyRepository().getOrCreateProperty(propertyDefinition, systemUser, null);
        getOntologyRepository().clearCache();

        workspace = getWorkspaceRepository().add("ws1", "workspace 1", user);
        workspaceAuthorizations = getAuthorizationRepository().getGraphAuthorizations(user, workspace.getWorkspaceId());
    }

    private WorkspaceHelper getWorkspaceHelper() {
        if (workspaceHelper == null) {
            workspaceHelper = new WorkspaceHelper(
                    getTermMentionRepository(),
                    getWorkQueueRepository(),
                    getGraph(),
                    getOntologyRepository(),
                    getWorkspaceRepository(),
                    getPrivilegeRepository(),
                    getAuthorizationRepository()
            );
        }

        return workspaceHelper;
    }

    @Override
    protected abstract WorkspaceRepository getWorkspaceRepository();

    @Test
    public void testUpdateProductExtendedData() {
        String kind = MockWorkProduct.class.getName();
        JSONObject params = new JSONObject();
        JSONObject extendedData = new JSONObject();
        extendedData.put("key1", "value1");
        extendedData.put("key2", "value2");
        extendedData.put("key3", new JSONObject("{\"a\":\"b\"}"));
        params.put("extendedData", extendedData);
        Product product = getWorkspaceRepository().addOrUpdateProduct(
                workspace.getWorkspaceId(),
                "p1",
                "product 1",
                kind,
                params,
                user
        );
        String productId = product.getId();

        product = getWorkspaceRepository().findProductById(workspace.getWorkspaceId(), productId, new JSONObject(), true, user);
        assertEquals("value1", product.getExtendedData().get("key1"));
        assertEquals("value2", product.getExtendedData().get("key2"));
        Object value3 = product.getExtendedData().get("key3");
        assertTrue(value3 instanceof Map);
        assertEquals("b", ((Map) value3).get("a"));

        // update extended data
        params = new JSONObject();
        extendedData = new JSONObject();
        extendedData.put("key1", JSONObject.NULL);
        extendedData.put("key2", "value2b");
        extendedData.put("key3", new JSONObject("{\"a\":\"b2\"}"));
        params.put("extendedData", extendedData);
        getWorkspaceRepository().addOrUpdateProduct(workspace.getWorkspaceId(), productId, null, null, params, user);

        product = getWorkspaceRepository().findProductById(workspace.getWorkspaceId(), productId, new JSONObject(), true, user);
        assertEquals(null, product.getExtendedData().get("key1"));
        assertEquals("value2b", product.getExtendedData().get("key2"));
        value3 = product.getExtendedData().get("key3");
        assertTrue(value3 instanceof Map);
        assertEquals("b2", ((Map) value3).get("a"));
    }

    @Test
    public void testPublishingNewVertexWithUnknownConcept() {
        doTestPublishVertexAdd("junit-missing", "Unable to locate concept with IRI junit-missing", null);
    }

    @Test
    public void testPublishingNewVertex() {
        doTestPublishVertexAdd(JUNIT_CONCEPT_TYPE, null, SandboxStatus.PUBLIC);
    }

    @Test
    public void testPublishingNewVertexAndConceptWithoutOntologyPublishPrivilege() {
        UserPropertyPrivilegeRepository privilegeRepository = (UserPropertyPrivilegeRepository) getPrivilegeRepository();
        privilegeRepository.setPrivileges(user, Sets.newHashSet(Privilege.ONTOLOGY_ADD), getUserRepository().getSystemUser());

        String newConceptIri = "new-concept";
        getOntologyRepository().getOrCreateConcept(thingConcept, newConceptIri, "Junit Concept", null, user, workspace.getWorkspaceId());
        getOntologyRepository().clearCache();

        doTestPublishVertexAdd(newConceptIri, "Unable to publish concept Junit Concept", SandboxStatus.PRIVATE);
    }

    @Test
    public void testPublishingNewVertexAndConcept() {
        UserPropertyPrivilegeRepository privilegeRepository = (UserPropertyPrivilegeRepository) getPrivilegeRepository();
        privilegeRepository.setPrivileges(user, Sets.newHashSet(Privilege.ONTOLOGY_ADD, Privilege.ONTOLOGY_PUBLISH), getUserRepository().getSystemUser());

        String newConceptIri = "new-concept";
        getOntologyRepository().getOrCreateConcept(thingConcept, newConceptIri, "Junit Concept", null, user, workspace.getWorkspaceId());
        getOntologyRepository().clearCache();

        doTestPublishVertexAdd(newConceptIri, null, SandboxStatus.PUBLIC);
    }

    @Test
    public void testPublishDeletedVertex() {
        User systemUser = getUserRepository().getSystemUser();
        Authorizations systemAuthorizations = getAuthorizationRepository().getGraphAuthorizations(systemUser);
        Vertex vertex = getGraphRepository().addVertex("v1", JUNIT_CONCEPT_TYPE, "", null, null, null, systemUser, systemAuthorizations);
        getWorkspaceRepository().updateEntitiesOnWorkspace(workspace, Collections.singleton(vertex.getId()), user);

        getWorkspaceHelper().deleteVertex(vertex, workspace.getWorkspaceId(), true, Priority.HIGH, workspaceAuthorizations, user);

        ClientApiVertexPublishItem publishItem = new ClientApiVertexPublishItem();
        publishItem.setVertexId(vertex.getId());
        publishItem.setAction(ClientApiPublishItem.Action.DELETE);

        List<ClientApiWorkspaceDiff.Item> diffs = getWorkspaceRepository().getDiff(workspace, user, null).getDiffs();
        assertEquals(1, diffs.size());

        assertNull(getGraph().getVertex(vertex.getId(), workspaceAuthorizations));
        assertNotNull(getGraph().getVertex(vertex.getId(), systemAuthorizations));

        ClientApiWorkspacePublishResponse response = getWorkspaceRepository().publish(new ClientApiPublishItem[]{publishItem}, user, workspace.getWorkspaceId(), workspaceAuthorizations);

        assertTrue(response.isSuccess());
        assertTrue(response.getFailures().isEmpty());

        assertNull(getGraph().getVertex(vertex.getId(), workspaceAuthorizations));
        assertNull(getGraph().getVertex(vertex.getId(), systemAuthorizations));

        diffs = getWorkspaceRepository().getDiff(workspace, user, null).getDiffs();
        assertEquals(0, diffs.size());
    }

    // TODO: Test auto publish properties

    private void doTestPublishVertexAdd(String conceptIri, String expectedError, SandboxStatus expectedConceptStatus) {
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(user, workspace.getWorkspaceId());

        Vertex vertex = getGraphRepository().addVertex("junit-vertex", conceptIri, "", workspace.getWorkspaceId(), null, null, user, authorizations);
        getWorkspaceRepository().updateEntityOnWorkspace(workspace, vertex.getId(), user);

        ClientApiVertexPublishItem publishItem = new ClientApiVertexPublishItem();
        publishItem.setVertexId(vertex.getId());
        publishItem.setAction(ClientApiPublishItem.Action.ADD_OR_UPDATE);

        ClientApiWorkspacePublishResponse response = getWorkspaceRepository().publish(new ClientApiPublishItem[]{publishItem}, user, workspace.getWorkspaceId(), authorizations);

        if (expectedError != null) {
            assertPublishFailure(response, workspace, getGraph().getVertex(vertex.getId(), authorizations), expectedError);
        } else {
            assertPublishSuccess(response, workspace, getGraph().getVertex(vertex.getId(), authorizations));
        }

        if (expectedConceptStatus != null) {
            Concept concept = getOntologyRepository().getConceptByIRI(conceptIri, user, workspace.getWorkspaceId());
            assertEquals(expectedConceptStatus, concept.getSandboxStatus());
        }
    }

    @Test
    public void testPublishingNewEdgeWithUnknownRelationship() {
        doTestPublishEdgeAdd("junit-missing", "Unable to locate relationship with IRI junit-missing", null);
    }

    @Test
    public void testPublishingNewEdge() {
        doTestPublishEdgeAdd(JUNIT_EDGE_LABEL, null, SandboxStatus.PUBLIC);
    }

    @Test
    public void testPublishingNewEdgeAndRelationshipWithoutOntologyPublishPrivilege() {
        UserPropertyPrivilegeRepository privilegeRepository = (UserPropertyPrivilegeRepository) getPrivilegeRepository();
        privilegeRepository.setPrivileges(user, Sets.newHashSet(Privilege.ONTOLOGY_ADD), getUserRepository().getSystemUser());

        String newRelationshipIri = "new-relationship";
        getOntologyRepository().getOrCreateRelationshipType(null, Collections.singleton(thingConcept), Collections.singleton(thingConcept), newRelationshipIri, "Junit Relationship", true, user, workspace.getWorkspaceId());
        getOntologyRepository().clearCache();

        doTestPublishEdgeAdd(newRelationshipIri, "Unable to publish relationship Junit Relationship", SandboxStatus.PRIVATE);
    }

    @Test
    public void testPublishingNewEdgeAndRelationship() {
        UserPropertyPrivilegeRepository privilegeRepository = (UserPropertyPrivilegeRepository) getPrivilegeRepository();
        privilegeRepository.setPrivileges(user, Sets.newHashSet(Privilege.ONTOLOGY_ADD, Privilege.ONTOLOGY_PUBLISH), getUserRepository().getSystemUser());

        String newRelationshipIri = "new-relationship";
        getOntologyRepository().getOrCreateRelationshipType(null, Collections.singleton(thingConcept), Collections.singleton(thingConcept), newRelationshipIri, "Junit Relationship", true, user, workspace.getWorkspaceId());
        getOntologyRepository().clearCache();

        doTestPublishEdgeAdd(newRelationshipIri, null, SandboxStatus.PUBLIC);
    }

    @Test
    public void testPublishDeletedEdge() {
        User systemUser = getUserRepository().getSystemUser();
        Authorizations systemAuthorizations = getAuthorizationRepository().getGraphAuthorizations(systemUser);
        Vertex v1 = getGraphRepository().addVertex("v1", JUNIT_CONCEPT_TYPE, "", null, null, null, systemUser, systemAuthorizations);
        Vertex v2 = getGraphRepository().addVertex("v2", JUNIT_CONCEPT_TYPE, "", null, null, null, systemUser, systemAuthorizations);
        Edge edge = getGraphRepository().addEdge("e1", v1, v2, JUNIT_EDGE_LABEL, null, null, null, null, systemUser, systemAuthorizations);
        getWorkspaceRepository().updateEntitiesOnWorkspace(workspace, Arrays.asList(v1.getId(), v2.getId()), user);

        getWorkspaceHelper().deleteEdge(workspace.getWorkspaceId(), edge, v1, v2, true, Priority.HIGH, workspaceAuthorizations, user);

        ClientApiRelationshipPublishItem publishItem = new ClientApiRelationshipPublishItem();
        publishItem.setEdgeId(edge.getId());
        publishItem.setAction(ClientApiPublishItem.Action.DELETE);

        List<ClientApiWorkspaceDiff.Item> diffs = getWorkspaceRepository().getDiff(workspace, user, null).getDiffs();
        assertEquals(1, diffs.size());

        assertNull(getGraph().getEdge(edge.getId(), workspaceAuthorizations));
        assertNotNull(getGraph().getEdge(edge.getId(), systemAuthorizations));

        ClientApiWorkspacePublishResponse response = getWorkspaceRepository().publish(new ClientApiPublishItem[]{publishItem}, user, workspace.getWorkspaceId(), workspaceAuthorizations);

        assertTrue(response.isSuccess());
        assertTrue(response.getFailures().isEmpty());

        assertNull(getGraph().getEdge(edge.getId(), workspaceAuthorizations));
        assertNull(getGraph().getEdge(edge.getId(), systemAuthorizations));

        diffs = getWorkspaceRepository().getDiff(workspace, user, null).getDiffs();
        assertEquals(0, diffs.size());
    }


    private void doTestPublishEdgeAdd(String edgeLabel, String expectedError, SandboxStatus expectedEdgeStatus) {
        User systemUser = getUserRepository().getSystemUser();
        Authorizations systemAuthorizations = getAuthorizationRepository().getGraphAuthorizations(systemUser);
        Vertex v1 = getGraphRepository().addVertex("v1", JUNIT_CONCEPT_TYPE, "", null, null, null, systemUser, systemAuthorizations);
        Vertex v2 = getGraphRepository().addVertex("v2", JUNIT_CONCEPT_TYPE, "", null, null, null, systemUser, systemAuthorizations);
        getWorkspaceRepository().updateEntitiesOnWorkspace(workspace, Arrays.asList(v1.getId(), v2.getId()), user);

        Edge edge = getGraphRepository().addEdge("e1", v1, v2, edgeLabel, null, null, "", workspace.getWorkspaceId(), user, workspaceAuthorizations);

        ClientApiRelationshipPublishItem publishItem = new ClientApiRelationshipPublishItem();
        publishItem.setEdgeId(edge.getId());
        publishItem.setAction(ClientApiPublishItem.Action.ADD_OR_UPDATE);

        ClientApiWorkspacePublishResponse response = getWorkspaceRepository().publish(new ClientApiPublishItem[]{publishItem}, user, workspace.getWorkspaceId(), workspaceAuthorizations);

        if (expectedError != null) {
            assertPublishFailure(response, workspace, getGraph().getEdge(edge.getId(), workspaceAuthorizations), expectedError);
        } else {
            assertPublishSuccess(response, workspace, getGraph().getEdge(edge.getId(), workspaceAuthorizations));
        }

        if (expectedEdgeStatus != null) {
            Relationship relationship = getOntologyRepository().getRelationshipByIRI(edgeLabel, user, workspace.getWorkspaceId());
            assertEquals(expectedEdgeStatus, relationship.getSandboxStatus());
        }
    }

    @Test
    public void testPublishingNewPropertyWithUnknownIri() {
        doTestPublishPropertyAdd("junit-missing", "Unable to locate property with IRI junit-missing", null);
    }

    @Test
    public void testPublishingNewProperty() {
        doTestPublishPropertyAdd(JUNIT_PROPERTY_NAME, null, SandboxStatus.PUBLIC);
    }

    @Test
    public void testPublishingNewPropertyValueAndPropertyTypeWithoutOntologyPublishPrivilege() {
        UserPropertyPrivilegeRepository privilegeRepository = (UserPropertyPrivilegeRepository) getPrivilegeRepository();
        privilegeRepository.setPrivileges(user, Sets.newHashSet(Privilege.ONTOLOGY_ADD), getUserRepository().getSystemUser());

        String newPropertyIri = "new-property";
        OntologyPropertyDefinition propertyDefinition = new OntologyPropertyDefinition(Collections.singletonList(thingConcept), newPropertyIri, "New Property", PropertyType.STRING);
        propertyDefinition.setTextIndexHints(Collections.singleton(TextIndexHint.EXACT_MATCH));
        propertyDefinition.setUserVisible(true);
        getOntologyRepository().getOrCreateProperty(propertyDefinition, user, workspace.getWorkspaceId());
        getOntologyRepository().clearCache();

        doTestPublishPropertyAdd(newPropertyIri, "Unable to publish relationship New Property", SandboxStatus.PRIVATE);
    }

    @Test
    public void testPublishingNewPropertyValueAndPropertyType() {
        UserPropertyPrivilegeRepository privilegeRepository = (UserPropertyPrivilegeRepository) getPrivilegeRepository();
        privilegeRepository.setPrivileges(user, Sets.newHashSet(Privilege.ONTOLOGY_ADD, Privilege.ONTOLOGY_PUBLISH), getUserRepository().getSystemUser());

        String newPropertyIri = "new-property";
        OntologyPropertyDefinition propertyDefinition = new OntologyPropertyDefinition(Collections.singletonList(thingConcept), newPropertyIri, "New Property", PropertyType.STRING);
        propertyDefinition.setTextIndexHints(Collections.singleton(TextIndexHint.EXACT_MATCH));
        propertyDefinition.setUserVisible(true);
        getOntologyRepository().getOrCreateProperty(propertyDefinition, user, workspace.getWorkspaceId());
        getOntologyRepository().clearCache();

        doTestPublishPropertyAdd(newPropertyIri, null, SandboxStatus.PUBLIC);
    }

    private void doTestPublishPropertyAdd(String propertyName, String expectedError, SandboxStatus expectedEdgeStatus) {
        User systemUser = getUserRepository().getSystemUser();
        Authorizations systemAuthorizations = getAuthorizationRepository().getGraphAuthorizations(systemUser);
        Vertex vertex = getGraphRepository().addVertex("v1", JUNIT_CONCEPT_TYPE, "", null, null, null, systemUser, systemAuthorizations);
        getWorkspaceRepository().updateEntityOnWorkspace(workspace, vertex.getId(), user);

        String propertyKey = "junit";
        VisibilityAndElementMutation<Vertex> setPropertyMutation = getGraphRepository().setProperty(vertex, propertyName, propertyKey, "new value", new Metadata(), "", "", workspace.getWorkspaceId(), null, null, user, workspaceAuthorizations);
        setPropertyMutation.elementMutation.save(workspaceAuthorizations);

        ClientApiPropertyPublishItem publishItem = new ClientApiPropertyPublishItem();
        publishItem.setVertexId(vertex.getId());
        publishItem.setName(propertyName);
        publishItem.setKey(propertyKey);
        publishItem.setAction(ClientApiPublishItem.Action.ADD_OR_UPDATE);

        ClientApiWorkspacePublishResponse response = getWorkspaceRepository().publish(new ClientApiPublishItem[]{publishItem}, user, workspace.getWorkspaceId(), workspaceAuthorizations);

        Property property = getGraph().getVertex(vertex.getId(), workspaceAuthorizations).getProperty(propertyKey, propertyName);
        if (expectedError != null) {
            assertPublishFailure(response, workspace, property, expectedError);
        } else {
            assertPublishSuccess(response, workspace, property);
        }

        if (expectedEdgeStatus != null) {
            OntologyProperty ontologyProperty = getOntologyRepository().getPropertyByIRI(propertyName, user, workspace.getWorkspaceId());
            assertEquals(expectedEdgeStatus, ontologyProperty.getSandboxStatus());
        }
    }

    private void assertPublishSuccess(
            ClientApiWorkspacePublishResponse response,
            Workspace workspace,
            Object vertexiumObject
    ) {
        assertTrue(response.isSuccess());
        assertTrue(response.getFailures().isEmpty());

        if (vertexiumObject instanceof Element) {
            assertEquals(SandboxStatus.PUBLIC, SandboxStatusUtil.getSandboxStatus((Element)vertexiumObject, workspace.getWorkspaceId()));
        } else {
            SandboxStatus[] propertySandboxStatuses = SandboxStatusUtil.getPropertySandboxStatuses(Collections.singletonList((Property) vertexiumObject), workspace.getWorkspaceId());
            assertEquals(1, propertySandboxStatuses.length);
            assertEquals(SandboxStatus.PUBLIC, propertySandboxStatuses[0]);
        }

        List<ClientApiWorkspaceDiff.Item> diffs = getWorkspaceRepository().getDiff(workspace, user, null).getDiffs();
        assertEquals(0, diffs.size());
    }

    private void assertPublishFailure(
            ClientApiWorkspacePublishResponse response,
            Workspace workspace,
            Object vertexiumObject,
            String expectedError
    ) {
        assertFalse(response.isSuccess());

        List<ClientApiPublishItem> failures = response.getFailures();
        assertEquals(1, failures.size());
        assertEquals(expectedError, failures.get(0).getErrorMessage());

        if (vertexiumObject instanceof Element) {
            assertEquals(SandboxStatus.PRIVATE, SandboxStatusUtil.getSandboxStatus((Element)vertexiumObject, workspace.getWorkspaceId()));
        } else {
            SandboxStatus[] propertySandboxStatuses = SandboxStatusUtil.getPropertySandboxStatuses(Collections.singletonList((Property) vertexiumObject), workspace.getWorkspaceId());
            assertEquals(1, propertySandboxStatuses.length);
            assertEquals(SandboxStatus.PRIVATE, propertySandboxStatuses[0]);
        }

        List<ClientApiWorkspaceDiff.Item> diffs = getWorkspaceRepository().getDiff(workspace, user, null).getDiffs();
        assertEquals(1, diffs.size());

        String diffItemType = vertexiumObject instanceof Element ?
                (vertexiumObject instanceof Vertex ? "VertexDiffItem" : "EdgeDiffItem") :
                "PropertyDiffItem";
        assertEquals(diffItemType, diffs.get(0).getType());
    }
}
