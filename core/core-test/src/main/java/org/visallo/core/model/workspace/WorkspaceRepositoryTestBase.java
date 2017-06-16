package org.visallo.core.model.workspace;

import org.json.JSONObject;
import org.junit.Test;
import org.vertexium.*;
import org.visallo.core.model.graph.VisibilityAndElementMutation;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyPropertyDefinition;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.workspace.product.Product;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloInMemoryTestBase;
import org.visallo.web.clientapi.model.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public abstract class WorkspaceRepositoryTestBase extends VisalloInMemoryTestBase {
    private static final String JUNIT_CONCEPT_TYPE = "junit-concept-iri";
    private static final String JUNIT_EDGE_LABEL = "junit-edge-iri";
    private static final String JUNIT_PROPERTY_NAME = "junit-property-iri";

    private User user;
    private Workspace workspace;
    private Authorizations workspaceAuthorizations;

    @Override
    public void before() {
        super.before();
        user = getUserRepository().findOrAddUser("base-junit", "Base Junit", "base-junit@visallo.com", "password");

        User systemUser = getUserRepository().getSystemUser();
        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(systemUser);
        Concept thing = getOntologyRepository().getEntityConcept();

        List<Concept> things = Collections.singletonList(thing);
        Relationship hasEntityRel = getOntologyRepository().getOrCreateRelationshipType(null, things, things, "has-entity-iri", true);
        hasEntityRel.addIntent("entityHasImage", authorizations);

        getOntologyRepository().getOrCreateConcept(thing, JUNIT_CONCEPT_TYPE, "Junit Concept", null);
        getOntologyRepository().getOrCreateRelationshipType(null, things, things, JUNIT_EDGE_LABEL, true);
        OntologyPropertyDefinition propertyDefinition = new OntologyPropertyDefinition(things, JUNIT_PROPERTY_NAME, "Junit Property", PropertyType.STRING);
        propertyDefinition.setTextIndexHints(Collections.singleton(TextIndexHint.EXACT_MATCH));
        propertyDefinition.setUserVisible(true);
        getOntologyRepository().getOrCreateProperty(propertyDefinition);
        getOntologyRepository().clearCache();

        workspace = getWorkspaceRepository().add("ws1", "workspace 1", user);
        workspaceAuthorizations = getAuthorizationRepository().getGraphAuthorizations(user, workspace.getWorkspaceId());
    }

    protected boolean supportsWorkProducts() {
        return true;
    }

    @Test
    public void testFindByIdNotExists() {
        Workspace ws = getWorkspaceRepository().findById("workspaceNotExists", false, user);
        assertEquals(null, ws);
    }

    @Test
    public void testUpdateProductExtendedData() {
        assumeTrue(supportsWorkProducts());

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
    public void testPublishPropertyWithoutChange() {
        Visibility defaultVisibility = getVisibilityTranslator().getDefaultVisibility();
        Vertex entity1Vertex = getGraph().prepareVertex("entity1Id", defaultVisibility)
                .addPropertyValue("key1", JUNIT_PROPERTY_NAME, "value1", new Metadata(), defaultVisibility)
                .addPropertyValue("key9", "prop9", "value9", new Metadata(), defaultVisibility)
                .save(getAuthorizationRepository().getGraphAuthorizations(getUserRepository().getSystemUser()));

        ClientApiPublishItem[] publishDate = new ClientApiPublishItem[1];
        publishDate[0] = new ClientApiPropertyPublishItem() {{
            setAction(Action.ADD_OR_UPDATE);
            setKey("key1");
            setName(JUNIT_PROPERTY_NAME);
            setVertexId(entity1Vertex.getId());
        }};

        Authorizations noAuthorizations = getAuthorizationRepository().getGraphAuthorizations(user);
        ClientApiWorkspacePublishResponse response = getWorkspaceRepository().publish(
                publishDate,
                workspace.getWorkspaceId(),
                noAuthorizations
        );

        assertEquals(1, response.getFailures().size());
        assertEquals(ClientApiPublishItem.Action.ADD_OR_UPDATE, response.getFailures().get(0).getAction());
        assertEquals("property", response.getFailures().get(0).getType());
        assertEquals(
                "no property with key 'key1' and name '" + JUNIT_PROPERTY_NAME + "' found on workspace '" + workspace.getWorkspaceId() + "'",
                response.getFailures().get(0).getErrorMessage()
        );
    }

    @Test
    public void testPublishPropertyWithChange() {
        Visibility defaultVisibility = getVisibilityTranslator().getDefaultVisibility();
        Vertex entity1Vertex = getGraph().prepareVertex("entity1Id", defaultVisibility)
                .addPropertyValue("key1", JUNIT_PROPERTY_NAME, "value1", new Metadata(), defaultVisibility)
                .addPropertyValue("key9", "prop9", "value9", new Metadata(), defaultVisibility)
                .save(getAuthorizationRepository().getGraphAuthorizations(getUserRepository().getSystemUser()));

        VisibilityAndElementMutation<Vertex> setPropertyResult = getGraphRepository().setProperty(
                entity1Vertex,
                JUNIT_PROPERTY_NAME,
                "key1",
                "newValue",
                new Metadata(),
                "",
                "",
                workspace.getWorkspaceId(),
                "I changed it",
                new ClientApiSourceInfo(),
                user,
                workspaceAuthorizations
        );
        setPropertyResult.elementMutation.save(workspaceAuthorizations);
        getGraph().flush();

        ClientApiPublishItem[] publishDate = new ClientApiPublishItem[1];
        publishDate[0] = new ClientApiPropertyPublishItem() {{
            setAction(Action.ADD_OR_UPDATE);
            setKey("key1");
            setName(JUNIT_PROPERTY_NAME);
            setVertexId(entity1Vertex.getId());
        }};
        ClientApiWorkspacePublishResponse response = getWorkspaceRepository().publish(
                publishDate,
                workspace.getWorkspaceId(),
                workspaceAuthorizations
        );
        if (response.getFailures().size() > 0) {
            String failMessage = "Had " + response.getFailures().size() + " failure(s): " + ": " + response.getFailures().get(
                    0).getErrorMessage();
            assertEquals(failMessage, 0, response.getFailures().size());
        }
    }
}
