package org.visallo.vertexium.model.workspace;

import org.junit.Test;
import org.vertexium.*;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.graph.VisibilityAndElementMutation;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyPropertyDefinition;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.*;
import org.visallo.core.model.workspace.product.WorkProduct;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.vertexium.util.IterableUtils.count;
import static org.vertexium.util.IterableUtils.toList;

public class VertexiumWorkspaceRepositoryTest extends WorkspaceRepositoryTestBase {
    private User user;
    private User otherUser;
    private Vertex entity1Vertex;

    private VertexiumWorkspaceRepository workspaceRepository;

    protected WorkspaceRepository getWorkspaceRepository() {
        if (workspaceRepository != null) {
            return workspaceRepository;
        }
        workspaceRepository = new VertexiumWorkspaceRepository(
                getGraph(),
                getConfiguration(),
                getGraphRepository(),
                getUserRepository(),
                getGraphAuthorizationRepository(),
                getWorkspaceDiffHelper(),
                getLockRepository(),
                getVisibilityTranslator(),
                getTermMentionRepository(),
                getOntologyRepository(),
                getWorkQueueRepository(),
                getAuthorizationRepository()
        ) {
            @Override
            protected Collection<WorkspaceListener> getWorkspaceListeners() {
                return VertexiumWorkspaceRepositoryTest.this.getWorkspaceListeners();
            }
        };

        List<WorkProduct> workProducts = new ArrayList<>();
        workProducts.add(new MockWorkProduct());
        workspaceRepository.setWorkProducts(workProducts);

        return workspaceRepository;
    }

    @Override
    public void before() {
        super.before();
        user = getUserRepository().findOrAddUser("junit-vwrt", "Junit VWRT", "junit-vwrt@visallo.com", "password");
        otherUser = getUserRepository().findOrAddUser("other-junit", "Other Junit", "other.junit@visallo.com", "password");

        User systemUser = getUserRepository().getSystemUser();
        Authorizations systemUserAuth = getAuthorizationRepository().getGraphAuthorizations(systemUser);
        Visibility defaultVisibility = getVisibilityTranslator().getDefaultVisibility();
        entity1Vertex = getGraph().prepareVertex("entity1Id", defaultVisibility)
                .addPropertyValue("key1", "prop1", "value1", new Metadata(), defaultVisibility)
                .addPropertyValue("key9", "prop9", "value9", new Metadata(), defaultVisibility)
                .save(systemUserAuth);


        Concept thing = getOntologyRepository().getEntityConcept(systemUser, null);
        OntologyPropertyDefinition propertyDefinition = new OntologyPropertyDefinition(Collections.singletonList(thing), "prop1", "Prop 1", PropertyType.STRING);
        propertyDefinition.setTextIndexHints(Collections.singleton(TextIndexHint.EXACT_MATCH));
        propertyDefinition.setUserVisible(true);
        getOntologyRepository().getOrCreateProperty(propertyDefinition, systemUser, null);
    }

    @Test
    public void testAddWorkspace() {
        Authorizations allAuths = getGraph().createAuthorizations(WorkspaceRepository.VISIBILITY_STRING);
        int startingVertexCount = count(getGraph().getVertices(allAuths));
        int startingEdgeCount = count(getGraph().getEdges(allAuths));

        String workspaceId = "testWorkspaceId";
        Workspace workspace = getWorkspaceRepository().add(workspaceId, "workspace1", user);
        assertTrue(getGraphAuthorizationRepository().getGraphAuthorizations().contains(workspaceId));

        assertEquals(startingVertexCount + 1, count(getGraph().getVertices(allAuths))); // +1 = the workspace vertex
        assertEquals(
                startingEdgeCount + 1,
                count(getGraph().getEdges(allAuths))
        ); // +1 = the edge between workspace and user1

        Authorizations noAuthorizations = getAuthorizationRepository().getGraphAuthorizations(user);
        assertNull("Should not have access", getGraph().getVertex(workspaceId, noAuthorizations));

        Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(user,
                WorkspaceRepository.VISIBILITY_STRING,
                workspace.getWorkspaceId());
        assertNotNull("Should have access", getGraph().getVertex(workspaceId, authorizations));

        Workspace foundWorkspace = getWorkspaceRepository().findById(workspaceId, user);
        assertEquals(workspaceId, foundWorkspace.getWorkspaceId());
    }

    @Test
    public void testFindByIdNotExists() {
        Workspace ws = getWorkspaceRepository().findById("workspaceNotExists", false, user);
        assertEquals(null, ws);
    }

    @Test
    public void testAccessControl() {
        Authorizations allAuths = getGraph().createAuthorizations(
                WorkspaceRepository.VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );
        int startingVertexCount = count(getGraph().getVertices(allAuths));
        int startingEdgeCount = count(getGraph().getEdges(allAuths));

        String workspace1Id = "testWorkspace1Id";
        String workspace1Title = "workspace1";
        getWorkspaceRepository().add(workspace1Id, workspace1Title, user);

        String workspace2Id = "testWorkspace2Id";
        String workspace2Title = "workspace2";
        getWorkspaceRepository().add(workspace2Id, workspace2Title, user);

        String workspace3Id = "testWorkspace3Id";
        String workspace3Title = "workspace3";
        getWorkspaceRepository().add(workspace3Id, workspace3Title, otherUser);

        assertEquals(startingVertexCount + 3, count(getGraph().getVertices(allAuths))); // +3 = the workspace vertices
        assertEquals(
                startingEdgeCount + 3,
                count(getGraph().getEdges(allAuths))
        ); // +3 = the edges between workspaces and users

        List<Workspace> user1Workspaces = toList(getWorkspaceRepository().findAllForUser(user));
        assertEquals(2, user1Workspaces.size());
        boolean foundWorkspace1 = false;
        boolean foundWorkspace2 = false;
        for (Workspace workspace : user1Workspaces) {
            if (workspace.getDisplayTitle().equals(workspace1Title)) {
                foundWorkspace1 = true;
            } else if (workspace.getDisplayTitle().equals(workspace2Title)) {
                foundWorkspace2 = true;
            }
        }
        assertTrue("foundWorkspace1", foundWorkspace1);
        assertTrue("foundWorkspace2", foundWorkspace2);

        List<Workspace> user2Workspaces = toList(getWorkspaceRepository().findAllForUser(otherUser));
        assertEquals(1, user2Workspaces.size());
        assertEquals(workspace3Title, user2Workspaces.get(0).getDisplayTitle());

        try {
            getWorkspaceRepository().updateUserOnWorkspace(
                    user2Workspaces.get(0),
                    user.getUserId(),
                    WorkspaceAccess.READ,
                    user
            );
            fail("user1 should not have access to user2's workspace");
        } catch (VisalloAccessDeniedException ex) {
            assertEquals(user, ex.getUser());
            assertEquals(user2Workspaces.get(0).getWorkspaceId(), ex.getResourceId());
        }

        WorkspaceRepository.UpdateUserOnWorkspaceResult updateUserOnWorkspaceResult = getWorkspaceRepository().updateUserOnWorkspace(
                user2Workspaces.get(0),
                user.getUserId(),
                WorkspaceAccess.READ,
                otherUser
        );
        assertEquals(WorkspaceRepository.UpdateUserOnWorkspaceResult.ADD, updateUserOnWorkspaceResult);
        assertEquals(startingVertexCount + 3, count(getGraph().getVertices(allAuths))); // +3 = the workspace vertices
        assertEquals(
                startingEdgeCount + 4,
                count(getGraph().getEdges(allAuths))
        ); // +4 = the edges between workspaces and users
        List<WorkspaceUser> usersWithAccess = getWorkspaceRepository().findUsersWithAccess(
                user2Workspaces.get(0).getWorkspaceId(),
                otherUser
        );
        boolean foundUser1 = false;
        boolean foundUser2 = false;
        for (WorkspaceUser userWithAccess : usersWithAccess) {
            if (userWithAccess.getUserId().equals(user.getUserId())) {
                assertEquals(WorkspaceAccess.READ, userWithAccess.getWorkspaceAccess());
                foundUser1 = true;
            } else if (userWithAccess.getUserId().equals(otherUser.getUserId())) {
                assertEquals(WorkspaceAccess.WRITE, userWithAccess.getWorkspaceAccess());
                foundUser2 = true;
            } else {
                fail("Unexpected user " + userWithAccess.getUserId());
            }
        }
        assertTrue("could not find user1", foundUser1);
        assertTrue("could not find user2", foundUser2);

        try {
            getWorkspaceRepository().deleteUserFromWorkspace(user2Workspaces.get(0), user.getUserId(), user);
            fail("user1 should not have write access to user2's workspace");
        } catch (VisalloAccessDeniedException ex) {
            assertEquals(user, ex.getUser());
            assertEquals(user2Workspaces.get(0).getWorkspaceId(), ex.getResourceId());
        }

        try {
            getWorkspaceRepository().delete(user2Workspaces.get(0), user);
            fail("user1 should not have write access to user2's workspace");
        } catch (VisalloAccessDeniedException ex) {
            assertEquals(user, ex.getUser());
            assertEquals(user2Workspaces.get(0).getWorkspaceId(), ex.getResourceId());
        }

        updateUserOnWorkspaceResult = getWorkspaceRepository().updateUserOnWorkspace(
                user2Workspaces.get(0),
                user.getUserId(),
                WorkspaceAccess.WRITE,
                otherUser
        );
        assertEquals(WorkspaceRepository.UpdateUserOnWorkspaceResult.UPDATE, updateUserOnWorkspaceResult);
        assertEquals(startingVertexCount + 3, count(getGraph().getVertices(allAuths))); // +3 = the workspace vertices
        assertEquals(
                startingEdgeCount + 4,
                count(getGraph().getEdges(allAuths))
        ); // +4 = the edges between workspaces and users

        getWorkspaceRepository().deleteUserFromWorkspace(user2Workspaces.get(0), user.getUserId(), otherUser);
        assertEquals(startingVertexCount + 3, count(getGraph().getVertices(allAuths))); // +3 = the workspace vertices
        assertEquals(
                startingEdgeCount + 3,
                count(getGraph().getEdges(allAuths))
        ); // +3 = the edges between workspaces and users

        getWorkspaceRepository().delete(user2Workspaces.get(0), otherUser);
        assertEquals(startingVertexCount + 2, count(getGraph().getVertices(allAuths))); // +2 = the workspace vertices
        assertEquals(
                startingEdgeCount + 2,
                count(getGraph().getEdges(allAuths))
        ); // +2 = the edges between workspaces and users
    }

    @Test
    public void testPublishPropertyWithoutChange() {
        String workspaceId = "testWorkspaceId";
        Workspace workspace = getWorkspaceRepository().add(workspaceId, "workspace1", user);

        ClientApiPublishItem[] publishDate = new ClientApiPublishItem[1];
        publishDate[0] = new ClientApiPropertyPublishItem() {{
            setAction(Action.ADD_OR_UPDATE);
            setKey("key1");
            setName("prop1");
            setVertexId(entity1Vertex.getId());
        }};

        Authorizations noAuthorizations = getAuthorizationRepository().getGraphAuthorizations(user);
        ClientApiWorkspacePublishResponse response = getWorkspaceRepository().publish(
                publishDate,
                user,
                workspace.getWorkspaceId(),
                noAuthorizations
        );

        assertEquals(1, response.getFailures().size());
        assertEquals(ClientApiPublishItem.Action.ADD_OR_UPDATE, response.getFailures().get(0).getAction());
        assertEquals("property", response.getFailures().get(0).getType());
        assertEquals(
                "no property with key 'key1' and name 'prop1' found on workspace 'testWorkspaceId'",
                response.getFailures().get(0).getErrorMessage()
        );
    }

    @Test
    public void testPublishPropertyWithChange() {
        String workspaceId = "testWorkspaceId";
        Workspace workspace = getWorkspaceRepository().add(workspaceId, "workspace1", user);

        Authorizations workspaceAuthorizations = getAuthorizationRepository().getGraphAuthorizations(user, workspaceId);
        VisibilityAndElementMutation<Vertex> setPropertyResult = getGraphRepository().setProperty(
                entity1Vertex,
                "prop1",
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
            setName("prop1");
            setVertexId(entity1Vertex.getId());
        }};
        ClientApiWorkspacePublishResponse response = getWorkspaceRepository().publish(
                publishDate,
                user,
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
