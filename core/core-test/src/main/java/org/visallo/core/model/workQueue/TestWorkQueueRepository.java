package org.visallo.core.model.workQueue;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.visallo.core.config.Configuration;
import org.visallo.core.ingest.WorkerSpout;
import org.visallo.core.ingest.graphProperty.GraphPropertyMessage;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.VisalloPropertyUpdate;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.status.model.Status;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloInMemoryTestBase;
import org.visallo.web.clientapi.model.ClientApiWorkspace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;


public class TestWorkQueueRepository extends WorkQueueRepository {
    public List<JSONObject> broadcastJsonValues = new ArrayList<>();
    public Map<String, List<byte[]>> queues = new HashMap<>();

    public TestWorkQueueRepository(
            Graph graph,
            WorkQueueNames workQueueNames,
            Configuration configuration
    ) {
        super(graph, workQueueNames, configuration);
    }

    @Override
    protected void broadcastJson(JSONObject json) {
        broadcastJsonValues.add(json);
    }

    @Override
    public void pushOnQueue(String queueName, byte[] data, Priority priority) {
        List<byte[]> queue = queues.get(queueName);
        if (queue == null) {
            queue = new ArrayList<>();
            queues.put(queueName, queue);
        }
        queue.add(data);
    }

    public List<byte[]> getWorkQueue(String queueName) {
        return queues.get(queueName);
    }

    public void clearQueue() {
        queues.clear();
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
