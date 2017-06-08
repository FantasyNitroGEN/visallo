package org.visallo.core.model.workspace;

import com.google.common.collect.Sets;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.vertexium.Authorizations;
import org.vertexium.Vertex;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.user.UserPropertyPrivilegeRepository;
import org.visallo.core.model.workspace.product.Product;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.core.util.VisalloInMemoryTestBase;
import org.visallo.web.clientapi.model.*;

import java.util.*;

import static org.junit.Assert.*;

public abstract class WorkspaceRepositoryTestBase extends VisalloInMemoryTestBase {
    private static final String JUNIT_CONCEPT_TYPE = "junit-concept-iri";

    private User user;

    @Before
    public void before() {
        super.before();
        user = getUserRepository().findOrAddUser("junit", "Junit", "junit@visallo.com", "password");

        User systemUser = getUserRepository().getSystemUser();
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(systemUser);
        Concept thing = getOntologyRepository().getEntityConcept(systemUser, null);

        Relationship hasEntityRel = getOntologyRepository().getOrCreateRelationshipType(null, Collections.singleton(thing), Collections.singleton(thing), "has-entity-iri", true, systemUser, null);
        hasEntityRel.addIntent("entityHasImage", authorizations);

        getOntologyRepository().getOrCreateConcept(thing, JUNIT_CONCEPT_TYPE, "Junit Concept", null, systemUser, null);
        getOntologyRepository().clearCache();
    }

    @Override
    protected abstract WorkspaceRepository getWorkspaceRepository();

    @Test
    public void testUpdateProductExtendedData() {
        Workspace workspace = getWorkspaceRepository().add("ws1", "workspace 1", user);
        String workspaceId = workspace.getWorkspaceId();

        String kind = MockWorkProduct.class.getName();
        JSONObject params = new JSONObject();
        JSONObject extendedData = new JSONObject();
        extendedData.put("key1", "value1");
        extendedData.put("key2", "value2");
        extendedData.put("key3", new JSONObject("{\"a\":\"b\"}"));
        params.put("extendedData", extendedData);
        Product product = getWorkspaceRepository().addOrUpdateProduct(
                workspaceId,
                "p1",
                "product 1",
                kind,
                params,
                user
        );
        String productId = product.getId();

        product = getWorkspaceRepository().findProductById(workspaceId, productId, new JSONObject(), true, user);
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
        getWorkspaceRepository().addOrUpdateProduct(workspaceId, productId, null, null, params, user);

        product = getWorkspaceRepository().findProductById(workspaceId, productId, new JSONObject(), true, user);
        assertEquals(null, product.getExtendedData().get("key1"));
        assertEquals("value2b", product.getExtendedData().get("key2"));
        value3 = product.getExtendedData().get("key3");
        assertTrue(value3 instanceof Map);
        assertEquals("b2", ((Map) value3).get("a"));
    }

    @Test
    public void testPublishingVerticesWithUnknownConcept() {
        doTestPublishVertexAdd("ws1", "junit-missing", "Unable to locate concept with IRI junit-missing", null);
    }

    @Test
    public void testPublishingVertices() {
        doTestPublishVertexAdd("ws1", JUNIT_CONCEPT_TYPE, null, SandboxStatus.PUBLIC);
    }

    @Test
    public void testPublishingVerticesAndConceptsWithoutPublishPrivilege() {
        String workspaceId = "ws";

        UserPropertyPrivilegeRepository privilegeRepository = (UserPropertyPrivilegeRepository) getPrivilegeRepository();
        privilegeRepository.setPrivileges(user, Sets.newHashSet(Privilege.ONTOLOGY_ADD), getUserRepository().getSystemUser());

        Concept thing = getOntologyRepository().getEntityConcept(getUserRepository().getSystemUser(), null);
        String newConceptIri = "new-concept";
        getOntologyRepository().getOrCreateConcept(thing, newConceptIri, "Junit Concept", null, user, workspaceId);
        getOntologyRepository().clearCache();

        doTestPublishVertexAdd(workspaceId, newConceptIri, "Unable to publish concept Junit Concept", SandboxStatus.PRIVATE);
    }

    @Test
    public void testPublishingVerticesAndConcepts() {
        String workspaceId = "ws";

        UserPropertyPrivilegeRepository privilegeRepository = (UserPropertyPrivilegeRepository) getPrivilegeRepository();
        privilegeRepository.setPrivileges(user, Sets.newHashSet(Privilege.ONTOLOGY_ADD, Privilege.ONTOLOGY_PUBLISH), getUserRepository().getSystemUser());

        Concept thing = getOntologyRepository().getEntityConcept(getUserRepository().getSystemUser(), null);
        String newConceptIri = "new-concept";
        getOntologyRepository().getOrCreateConcept(thing, newConceptIri, "Junit Concept", null, user, workspaceId);
        getOntologyRepository().clearCache();

        doTestPublishVertexAdd(workspaceId, newConceptIri, null, SandboxStatus.PUBLIC);
    }

    // TODO: Test auto publish properties

    private ClientApiWorkspacePublishResponse doTestPublishVertexAdd(String workspaceId, String conceptIri, String expectedError, SandboxStatus expectedConceptStatus) {
        Workspace workspace = getWorkspaceRepository().add(workspaceId, "workspace 1", user);
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(user, workspace.getWorkspaceId());

        Vertex vertex = getGraphRepository().addVertex("junit-vertex", conceptIri, "", workspace.getWorkspaceId(), null, null, user, authorizations);
        getWorkspaceRepository().updateEntityOnWorkspace(workspace, vertex.getId(), user);

        ClientApiVertexPublishItem publishItem = new ClientApiVertexPublishItem();
        publishItem.setVertexId(vertex.getId());
        publishItem.setAction(ClientApiPublishItem.Action.ADD_OR_UPDATE);

        ClientApiWorkspacePublishResponse response = getWorkspaceRepository().publish(new ClientApiPublishItem[]{publishItem}, user, workspace.getWorkspaceId(), authorizations);

        if (expectedError != null) {
            assertFalse(response.isSuccess());

            List<ClientApiPublishItem> failures = response.getFailures();
            assertEquals(1, failures.size());
            assertEquals(expectedError, failures.get(0).getErrorMessage());

            vertex = getGraph().getVertex(vertex.getId(), authorizations);
            assertEquals(SandboxStatus.PRIVATE, SandboxStatusUtil.getSandboxStatus(vertex, workspace.getWorkspaceId()));

            List<ClientApiWorkspaceDiff.Item> diffs = getWorkspaceRepository().getDiff(workspace, user, null).getDiffs();
            assertEquals(1, diffs.size());
            assertEquals("VertexDiffItem", diffs.get(0).getType());
        } else {
            assertTrue(response.isSuccess());
            assertTrue(response.getFailures().isEmpty());

            vertex = getGraph().getVertex(vertex.getId(), authorizations);
            assertEquals(SandboxStatus.PUBLIC, SandboxStatusUtil.getSandboxStatus(vertex, workspace.getWorkspaceId()));
        }

        if (expectedConceptStatus != null) {
            Concept concept = getOntologyRepository().getConceptByIRI(conceptIri, user, workspace.getWorkspaceId());
            assertEquals(expectedConceptStatus, concept.getSandboxStatus());
        }

        return response;
    }
}
