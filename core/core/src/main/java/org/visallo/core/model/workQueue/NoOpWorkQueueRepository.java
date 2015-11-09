package org.visallo.core.model.workQueue;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.WorkerSpout;
import org.visallo.core.model.FlushFlag;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.status.model.Status;

import java.util.HashMap;
import java.util.Map;

public class NoOpWorkQueueRepository extends WorkQueueRepository {
    @Inject
    protected NoOpWorkQueueRepository(Graph graph, WorkQueueNames workQueueNames, Configuration config) {
        super(graph, workQueueNames, config);
    }

    @Override
    protected void broadcastJson(JSONObject json) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json, Priority priority) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void flush() {
        throw new RuntimeException("not supported");
    }

    @Override
    public void format() {
        throw new RuntimeException("not supported");
    }

    @Override
    public void subscribeToBroadcastMessages(BroadcastConsumer broadcastConsumer) {

    }

    @Override
    public WorkerSpout createWorkerSpout(String queueName) {
        throw new VisalloException("Not supported");
    }

    @Override
    public Map<String, Status> getQueuesStatus() {
        return new HashMap<>();
    }
}
