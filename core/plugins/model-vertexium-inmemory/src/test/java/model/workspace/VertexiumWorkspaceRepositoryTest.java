package model.workspace;

import com.google.common.collect.Lists;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.vertexium.id.QueueIdGenerator;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.vertexium.inmemory.InMemoryGraph;
import org.vertexium.inmemory.InMemoryGraphConfiguration;
import org.vertexium.search.DefaultSearchIndex;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.lock.LocalLockRepository;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.notification.UserNotificationRepository;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.user.UserSessionCounterRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.*;
import org.visallo.core.model.workspace.diff.WorkspaceDiffHelper;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.vertexium.model.user.InMemoryAuthorizationRepository;
import org.visallo.vertexium.model.user.InMemoryUser;
import org.visallo.vertexium.model.user.InMemoryUserRepository;
import org.visallo.vertexium.model.workspace.VertexiumWorkspaceRepository;
import org.visallo.web.clientapi.model.GraphPosition;
import org.visallo.web.clientapi.model.WorkspaceAccess;

import java.util.HashMap;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;
import static org.vertexium.util.IterableUtils.count;
import static org.vertexium.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class VertexiumWorkspaceRepositoryTest {
    private InMemoryGraph graph;

    @Mock
    private WorkspaceDiffHelper workspaceDiff;

    private InMemoryUser user1;

    private InMemoryUser user2;

    private QueueIdGenerator idGenerator;

    private WorkspaceRepository workspaceRepository;
    private AuthorizationRepository authorizationRepository;
    private Vertex entity1Vertex;

    @Mock
    private SimpleOrmSession simpleOrmSession;

    @Mock
    private UserSessionCounterRepository userSessionCounterRepository;

    @Mock
    private WorkQueueRepository workQueueRepository;

    @Mock
    private UserNotificationRepository userNotificationRepository;

    @Before
    public void setup() throws Exception {
        Visibility visibility = new Visibility("");
        Authorizations authorizations = new InMemoryAuthorizations();
        InMemoryGraphConfiguration config = new InMemoryGraphConfiguration(new HashMap());
        idGenerator = new QueueIdGenerator();
        graph = InMemoryGraph.create(config, idGenerator, new DefaultSearchIndex(config));
        authorizationRepository = new InMemoryAuthorizationRepository();

        Configuration visalloConfiguration = new HashMapConfigurationLoader(new HashMap()).createConfiguration();
        LockRepository lockRepository = new LocalLockRepository();

        InMemoryUserRepository userRepository = new InMemoryUserRepository(
                graph,
                visalloConfiguration,
                simpleOrmSession,
                userSessionCounterRepository,
                workQueueRepository,
                userNotificationRepository
        );
        user1 = (InMemoryUser) userRepository.addUser("user2", "user2", null, "none", new String[0]);
        graph.addVertex(user1.getUserId(), visibility, authorizations);

        user2 = (InMemoryUser) userRepository.addUser("user2", "user2", null, "none", new String[0]);
        graph.addVertex(user2.getUserId(), visibility, authorizations);

        workspaceRepository = new VertexiumWorkspaceRepository(graph, userRepository, authorizationRepository, workspaceDiff, lockRepository);

        String entity1VertexId = "entity1Id";
        entity1Vertex = graph.addVertex(entity1VertexId, new VisalloVisibility().getVisibility(), new InMemoryAuthorizations());
    }

    @Test
    public void testAddWorkspace() {
        Authorizations allAuths = graph.createAuthorizations(WorkspaceRepository.VISIBILITY_STRING);
        int startingVertexCount = count(graph.getVertices(allAuths));
        int startingEdgeCount = count(graph.getEdges(allAuths));

        String workspaceId = "testWorkspaceId";
        idGenerator.push(workspaceId);
        idGenerator.push(workspaceId + "_to_" + user1.getUserId());

        Workspace workspace = workspaceRepository.add("workspace1", user1);
        assertTrue(authorizationRepository.getGraphAuthorizations().contains(WorkspaceRepository.WORKSPACE_ID_PREFIX + workspaceId));

        assertEquals(startingVertexCount + 1, count(graph.getVertices(allAuths))); // +1 = the workspace vertex
        assertEquals(startingEdgeCount + 1, count(graph.getEdges(allAuths))); // +1 = the edge between workspace and user1

        assertNull("Should not have access", graph.getVertex(workspace.getWorkspaceId(), new InMemoryAuthorizations()));
        InMemoryAuthorizations authorizations = new InMemoryAuthorizations(WorkspaceRepository.VISIBILITY_STRING, workspace.getWorkspaceId());
        assertNotNull("Should have access", graph.getVertex(workspace.getWorkspaceId(), authorizations));

        Workspace foundWorkspace = workspaceRepository.findById(workspace.getWorkspaceId(), user1);
        assertEquals(workspace.getWorkspaceId(), foundWorkspace.getWorkspaceId());
    }

    @Test
    public void testAccessControl() {
        Authorizations allAuths = graph.createAuthorizations(WorkspaceRepository.VISIBILITY_STRING, UserRepository.VISIBILITY_STRING);
        int startingVertexCount = count(graph.getVertices(allAuths));
        int startingEdgeCount = count(graph.getEdges(allAuths));

        String workspace1Id = "testWorkspace1Id";
        String workspace1Title = "workspace1";
        idGenerator.push(workspace1Id);
        idGenerator.push(workspace1Id + "_to_" + user1.getUserId());
        workspaceRepository.add(workspace1Title, user1);

        String workspace2Id = "testWorkspace2Id";
        String workspace2Title = "workspace2";
        idGenerator.push(workspace2Id);
        idGenerator.push(workspace2Id + "_to_" + user1.getUserId());
        workspaceRepository.add(workspace2Title, user1);

        String workspace3Id = "testWorkspace3Id";
        String workspace3Title = "workspace3";
        idGenerator.push(workspace3Id);
        idGenerator.push(workspace3Id + "_to_" + user2.getUserId());
        workspaceRepository.add(workspace3Title, user2);

        assertEquals(startingVertexCount + 3, count(graph.getVertices(allAuths))); // +3 = the workspace vertices
        assertEquals(startingEdgeCount + 3, count(graph.getEdges(allAuths))); // +3 = the edges between workspaces and users

        List<Workspace> user1Workspaces = toList(workspaceRepository.findAllForUser(user1));
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

        List<Workspace> user2Workspaces = toList(workspaceRepository.findAllForUser(user2));
        assertEquals(1, user2Workspaces.size());
        assertEquals(workspace3Title, user2Workspaces.get(0).getDisplayTitle());

        try {
            workspaceRepository.updateUserOnWorkspace(user2Workspaces.get(0), user1.getUserId(), WorkspaceAccess.READ, user1);
            fail("user1 should not have access to user2's workspace");
        } catch (VisalloAccessDeniedException ex) {
            assertEquals(user1, ex.getUser());
            assertEquals(user2Workspaces.get(0).getWorkspaceId(), ex.getResourceId());
        }

        idGenerator.push(workspace3Id + "to" + user2.getUserId());
        workspaceRepository.updateUserOnWorkspace(user2Workspaces.get(0), user1.getUserId(), WorkspaceAccess.READ, user2);
        assertEquals(startingVertexCount + 3, count(graph.getVertices(allAuths))); // +3 = the workspace vertices
        assertEquals(startingEdgeCount + 4, count(graph.getEdges(allAuths))); // +4 = the edges between workspaces and users
        List<WorkspaceUser> usersWithAccess = workspaceRepository.findUsersWithAccess(user2Workspaces.get(0).getWorkspaceId(), user2);
        boolean foundUser1 = false;
        boolean foundUser2 = false;
        for (WorkspaceUser userWithAccess : usersWithAccess) {
            if (userWithAccess.getUserId().equals(user1.getUserId())) {
                assertEquals(WorkspaceAccess.READ, userWithAccess.getWorkspaceAccess());
                foundUser1 = true;
            } else if (userWithAccess.getUserId().equals(user2.getUserId())) {
                assertEquals(WorkspaceAccess.WRITE, userWithAccess.getWorkspaceAccess());
                foundUser2 = true;
            } else {
                fail("Unexpected user " + userWithAccess.getUserId());
            }
        }
        assertTrue("could not find user1", foundUser1);
        assertTrue("could not find user2", foundUser2);

        try {
            workspaceRepository.deleteUserFromWorkspace(user2Workspaces.get(0), user1.getUserId(), user1);
            fail("user1 should not have write access to user2's workspace");
        } catch (VisalloAccessDeniedException ex) {
            assertEquals(user1, ex.getUser());
            assertEquals(user2Workspaces.get(0).getWorkspaceId(), ex.getResourceId());
        }

        try {
            workspaceRepository.delete(user2Workspaces.get(0), user1);
            fail("user1 should not have write access to user2's workspace");
        } catch (VisalloAccessDeniedException ex) {
            assertEquals(user1, ex.getUser());
            assertEquals(user2Workspaces.get(0).getWorkspaceId(), ex.getResourceId());
        }

        workspaceRepository.updateUserOnWorkspace(user2Workspaces.get(0), user1.getUserId(), WorkspaceAccess.WRITE, user2);
        assertEquals(startingVertexCount + 3, count(graph.getVertices(allAuths))); // +3 = the workspace vertices
        assertEquals(startingEdgeCount + 4, count(graph.getEdges(allAuths))); // +4 = the edges between workspaces and users

        workspaceRepository.deleteUserFromWorkspace(user2Workspaces.get(0), user1.getUserId(), user2);
        assertEquals(startingVertexCount + 3, count(graph.getVertices(allAuths))); // +3 = the workspace vertices
        assertEquals(startingEdgeCount + 3, count(graph.getEdges(allAuths))); // +3 = the edges between workspaces and users

        workspaceRepository.delete(user2Workspaces.get(0), user2);
        assertEquals(startingVertexCount + 2, count(graph.getVertices(allAuths))); // +2 = the workspace vertices
        assertEquals(startingEdgeCount + 2, count(graph.getEdges(allAuths))); // +2 = the edges between workspaces and users
    }

    @Test
    public void testEntities() {
        Authorizations allAuths = graph.createAuthorizations(WorkspaceRepository.VISIBILITY_STRING);
        int startingVertexCount = count(graph.getVertices(allAuths));
        int startingEdgeCount = count(graph.getEdges(allAuths));

        String workspaceId = "testWorkspaceId";
        idGenerator.push(workspaceId);
        idGenerator.push(workspaceId + "_to_" + user1.getUserId());

        Workspace workspace = workspaceRepository.add("workspace1", user1);
        assertEquals(startingVertexCount + 1, count(graph.getVertices(allAuths))); // +1 = the workspace vertex
        assertEquals(startingEdgeCount + 1, count(graph.getEdges(allAuths))); // +1 = the edges between workspaces and users

        try {
            workspaceRepository.updateEntityOnWorkspace(workspace, entity1Vertex.getId(), true, new GraphPosition(100, 100), user2);
            fail("user2 should not have write access to workspace");
        } catch (VisalloAccessDeniedException ex) {
            assertEquals(user2, ex.getUser());
            assertEquals(workspace.getWorkspaceId(), ex.getResourceId());
        }

        idGenerator.push(workspaceId + "_to_" + entity1Vertex.getId());
        workspaceRepository.updateEntityOnWorkspace(workspace, entity1Vertex.getId(), true, new GraphPosition(100, 200), user1);
        assertEquals(startingVertexCount + 1, count(graph.getVertices(allAuths))); // +1 = the workspace vertex
        assertEquals(startingEdgeCount + 2, count(graph.getEdges(allAuths))); // +2 = the edges between workspaces, users, and entities

        workspaceRepository.updateEntityOnWorkspace(workspace, entity1Vertex.getId(), true, new GraphPosition(200, 300), user1);
        assertEquals(startingVertexCount + 1, count(graph.getVertices(allAuths))); // +1 = the workspace vertex
        assertEquals(startingEdgeCount + 2, count(graph.getEdges(allAuths))); // +2 = the edges between workspaces, users, and entities

        List<WorkspaceEntity> entities = workspaceRepository.findEntities(workspace, user1);
        assertEquals(1, entities.size());
        assertEquals(entity1Vertex.getId(), entities.get(0).getEntityVertexId());
        assertEquals(200, entities.get(0).getGraphPositionX().intValue());
        assertEquals(300, entities.get(0).getGraphPositionY().intValue());

        try {
            workspaceRepository.findEntities(workspace, user2);
            fail("user2 should not have read access to workspace");
        } catch (VisalloAccessDeniedException ex) {
            assertEquals(user2, ex.getUser());
            assertEquals(workspace.getWorkspaceId(), ex.getResourceId());
        }

        try {
            workspaceRepository.softDeleteEntitiesFromWorkspace(workspace, Lists.newArrayList(entity1Vertex.getId()), user2);
            fail("user2 should not have write access to workspace");
        } catch (VisalloAccessDeniedException ex) {
            assertEquals(user2, ex.getUser());
            assertEquals(workspace.getWorkspaceId(), ex.getResourceId());
        }

        workspaceRepository.softDeleteEntitiesFromWorkspace(workspace, Lists.newArrayList(entity1Vertex.getId()), user1);
        assertEquals(startingVertexCount + 1, count(graph.getVertices(allAuths))); // +1 = the workspace vertex
        List<Edge> edgesAfterDelete = toList(graph.getEdges(allAuths));
        assertEquals(startingEdgeCount + 2, count(edgesAfterDelete)); // +1 = the edges between workspaces, users
        boolean foundRemovedEdge = false;
        for (Edge edge : edgesAfterDelete) {
            if (edge.getLabel().equals(VertexiumWorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI)) {
                assertEquals(false, WorkspaceProperties.WORKSPACE_TO_ENTITY_VISIBLE.getPropertyValue(edge));
                foundRemovedEdge = true;
            }
        }
        assertTrue(foundRemovedEdge);
    }
}
