package org.visallo.core.model.workQueue;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.config.Configuration;
import org.visallo.core.ingest.WorkerSpout;
import org.visallo.core.model.FlushFlag;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.status.model.Status;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiWorkspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkQueueRepositoryTest {
    private TestWorkQueueRepository workQueueRepository;
    private Graph graph;

    @Mock
    private WorkQueueNames workQueueNames;

    @Mock
    private Configuration configuration;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private User mockUser1;

    @Mock
    private User mockUser2;

    @Mock
    private Workspace workspace;

    @Before
    public void before() {
        graph = InMemoryGraph.create();
        workQueueRepository = new TestWorkQueueRepository(
                graph,
                workQueueNames,
                configuration,
                userRepository,
                authorizationRepository,
                workspaceRepository
        );
    }

    @Test
    public void testPushWorkspaceChangeSameUser() {
        ClientApiWorkspace workspace = new ClientApiWorkspace();
        List<ClientApiWorkspace.User> previousUsers = new ArrayList<>();
        ClientApiWorkspace.User previousUser = new ClientApiWorkspace.User();
        previousUser.setUserId("user123");
        previousUsers.add(previousUser);
        String changedByUserId = "user123";
        String changedBySourceGuid = "123-123-1234";

        workQueueRepository.pushWorkspaceChange(workspace, previousUsers, changedByUserId, changedBySourceGuid);

        assertEquals(1, workQueueRepository.broadcastJsonValues.size());
        JSONObject json = workQueueRepository.broadcastJsonValues.get(0);
        assertEquals("workspaceChange", json.getString("type"));
        assertEquals("user123", json.getString("modifiedBy"));
        assertEquals(new JSONObject("{\"users\":[\"user123\"]}").toString(), json.getJSONObject("permissions").toString());
        assertEquals(
                new JSONObject("{\"vertices\":[],\"editable\":false,\"active\":false,\"users\":[],\"commentable\":false,\"sharedToUser\":false}").toString(),
                json.getJSONObject("data").toString()
        );
        assertEquals("123-123-1234", json.getString("sourceGuid"));
    }

    @Test
    public void testPushWorkspaceChangeDifferentUser() {
        ClientApiWorkspace clientApiWorkspace = new ClientApiWorkspace();
        clientApiWorkspace.setWorkspaceId("ws1");
        List<ClientApiWorkspace.User> previousUsers = new ArrayList<>();
        ClientApiWorkspace.User previousUser = new ClientApiWorkspace.User();
        previousUser.setUserId("mockUser1");
        previousUsers.add(previousUser);
        String changedByUserId = "mockUser2";
        String changedBySourceGuid = "123-123-1234";

        Authorizations mockUser1Auths = graph.createAuthorizations("mockUser1Auths");

        when(userRepository.findById(changedByUserId)).thenReturn(mockUser2);
        when(workspaceRepository.findById(eq("ws1"), eq(mockUser2))).thenReturn(workspace);
        when(userRepository.findById(eq("mockUser1"))).thenReturn(mockUser1);
        when(authorizationRepository.getGraphAuthorizations(eq(mockUser1), eq("ws1"))).thenReturn(mockUser1Auths);
        when(workspaceRepository.toClientApi(eq(workspace), eq(mockUser1), any())).thenReturn(clientApiWorkspace);

        workQueueRepository.pushWorkspaceChange(clientApiWorkspace, previousUsers, changedByUserId, changedBySourceGuid);

        assertEquals(1, workQueueRepository.broadcastJsonValues.size());
        JSONObject json = workQueueRepository.broadcastJsonValues.get(0);
        assertEquals("workspaceChange", json.getString("type"));
        assertEquals("mockUser2", json.getString("modifiedBy"));
        assertEquals(new JSONObject("{\"users\":[\"mockUser1\"]}").toString(), json.getJSONObject("permissions").toString());
        assertEquals(
                new JSONObject("{\"vertices\":[],\"editable\":false,\"active\":false,\"users\":[],\"commentable\":false,\"workspaceId\":\"ws1\",\"sharedToUser\":false}").toString(),
                json.getJSONObject("data").toString()
        );
        assertEquals("123-123-1234", json.getString("sourceGuid"));
    }

    public class TestWorkQueueRepository extends WorkQueueRepository {
        public List<JSONObject> broadcastJsonValues = new ArrayList<>();

        public TestWorkQueueRepository(
                Graph graph,
                WorkQueueNames workQueueNames,
                Configuration configuration,
                UserRepository userRepository,
                AuthorizationRepository authorizationRepository,
                WorkspaceRepository workspaceRepository
        ) {
            super(graph, workQueueNames, configuration);
            setUserRepository(userRepository);
            setAuthorizationRepository(authorizationRepository);
            setWorkspaceRepository(workspaceRepository);
        }

        @Override
        protected void broadcastJson(JSONObject json) {
            broadcastJsonValues.add(json);
        }

        @Override
        public void pushOnQueue(String queueName, @Deprecated FlushFlag flushFlag, JSONObject json, Priority priority) {

        }

        @Override
        public void flush() {

        }

        @Override
        protected void deleteQueue(String queueName) {

        }

        @Override
        public void subscribeToBroadcastMessages(BroadcastConsumer broadcastConsumer) {

        }

        @Override
        public WorkerSpout createWorkerSpout(String queueName) {
            return null;
        }

        @Override
        public Map<String, Status> getQueuesStatus() {
            return null;
        }
    }
}
