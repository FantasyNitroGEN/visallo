package org.visallo.model.queue.inmemory;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.visallo.core.config.Configuration;
import org.visallo.core.ingest.WorkerSpout;
import org.visallo.core.ingest.WorkerTuple;
import org.visallo.core.model.FlushFlag;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.status.model.QueueStatus;
import org.visallo.core.status.model.Status;

import java.util.*;

public class InMemoryWorkQueueRepository extends WorkQueueRepository {
    private static Map<String, List<JSONObject>> queues = new HashMap<>();
    private List<BroadcastConsumer> broadcastConsumers = new ArrayList<>();

    @Inject
    public InMemoryWorkQueueRepository(
            Graph graph,
            WorkQueueNames workQueueNames,
            Configuration configuration,
            UserRepository userRepository,
            AuthorizationRepository authorizationRepository,
            WorkspaceRepository workspaceRepository
    ) {
        super(graph, workQueueNames, configuration, userRepository, authorizationRepository, workspaceRepository);
    }

    @Override
    protected void broadcastJson(JSONObject json) {
        for (BroadcastConsumer consumer : broadcastConsumers) {
            consumer.broadcastReceived(json);
        }
    }

    @Override
    public void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json, Priority priority) {
        LOGGER.debug("push on queue: %s: %s", queueName, json);
        addToQueue(queueName, json, priority);
    }

    public void addToQueue(String queueName, JSONObject json, Priority priority) {
        final List<JSONObject> queue = getQueue(queueName);
        // getQueue - only returns static variables
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (queue) {
            if (priority == Priority.HIGH) {
                queue.add(0, json);
            } else {
                queue.add(json);
            }
            queue.notifyAll();
        }
    }

    @Override
    public void flush() {

    }

    @Override
    public void format() {
        clearQueue();
    }

    @Override
    public void subscribeToBroadcastMessages(BroadcastConsumer broadcastConsumer) {
        broadcastConsumers.add(broadcastConsumer);
    }

    @Override
    public WorkerSpout createWorkerSpout(String queueName) {
        final List<JSONObject> queue = getQueue(queueName);
        return new WorkerSpout() {
            @Override
            public WorkerTuple nextTuple() throws Exception {
                synchronized (queue) {
                    if (queue.size() == 0) {
                        return null;
                    }
                    JSONObject entry = queue.remove(0);
                    if (entry == null) {
                        return null;
                    }
                    return new WorkerTuple("", entry);
                }
            }
        };
    }

    @Override
    public Map<String, Status> getQueuesStatus() {
        Map<String, Status> results = new HashMap<>();
        for (Map.Entry<String, List<JSONObject>> queue : queues.entrySet()) {
            results.put(queue.getKey(), new QueueStatus(queue.getValue().size()));
        }
        return results;
    }

    public static void clearQueue() {
        queues.clear();
    }

    @Override
    protected void deleteQueue(String queueName) {
        queues.remove(queueName);
    }

    public static List<JSONObject> getQueue(String queueName) {
        List<JSONObject> queue = queues.get(queueName);
        if (queue == null) {
            queue = new LinkedList<>();
            queues.put(queueName, queue);
        }
        return queue;
    }
}
