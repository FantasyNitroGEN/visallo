package org.visallo.core.model.hazelcast;

import com.google.inject.Inject;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.WorkerSpout;
import org.visallo.core.ingest.WorkerTuple;
import org.visallo.core.model.FlushFlag;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.status.model.QueueStatus;
import org.visallo.core.status.model.Status;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class HazelcastWorkQueueRepository extends WorkQueueRepository {
    /**
     * hazelcast doesn't need IDs to track queue items so this is just a
     *  dummy value to make workers happy with a non-null value.
     */
    private static final String DUMMY_MESSAGE_ID = "hazelcast";

    private final HazelcastRepository hazelcastRepository;
    private final ITopic<JSONObject> broadcastTopic;

    @Inject
    public HazelcastWorkQueueRepository(
            Graph graph,
            WorkQueueNames workQueueNames,
            Configuration configuration,
            HazelcastRepository hazelcastRepository
    ) {
        super(graph, workQueueNames, configuration);
        this.hazelcastRepository = hazelcastRepository;
        String broadcastTopicName = hazelcastRepository.getHazelcastConfiguration().getBroadcastTopicName();
        broadcastTopic = hazelcastRepository.getHazelcastInstance().getTopic(broadcastTopicName);
    }

    @Override
    protected void broadcastJson(JSONObject json) {
        broadcastTopic.publish(json);
    }

    @Override
    public void pushOnQueue(String queueName, @Deprecated FlushFlag flushFlag, JSONObject json, Priority priority) {
        BlockingQueue<JSONObject> queue = hazelcastRepository.getHazelcastInstance().getQueue(queueName);
        try {
            queue.put(json);
        } catch (InterruptedException ex) {
            throw new VisalloException("Could not add item to queue: " + json, ex);
        }
    }

    @Override
    public void flush() {

    }

    @Override
    protected void deleteQueue(String queueName) {
        hazelcastRepository.getHazelcastInstance().getQueue(queueName).clear();
    }

    @Override
    public void subscribeToBroadcastMessages(final BroadcastConsumer broadcastConsumer) {
        broadcastTopic.addMessageListener(new MessageListener<JSONObject>() {
            @Override
            public void onMessage(Message<JSONObject> message) {
                try {
                    broadcastConsumer.broadcastReceived(message.getMessageObject());
                } catch (Throwable ex) {
                    LOGGER.error("problem in broadcast thread", ex);
                }
            }
        });
    }

    @Override
    public WorkerSpout createWorkerSpout(String queueName) {
        final IQueue<JSONObject> queue = hazelcastRepository.getHazelcastInstance().getQueue(queueName);
        return new WorkerSpout() {
            @Override
            public WorkerTuple nextTuple() throws Exception {
                JSONObject json = queue.take();
                Object messageId = DUMMY_MESSAGE_ID;
                return new WorkerTuple(messageId, json);
            }
        };
    }

    @Override
    public Map<String, Status> getQueuesStatus() {
        Map<String, Status> result = new HashMap<>();
        for (String queueName : getQueueNames()) {
            IQueue<Object> queue = hazelcastRepository.getHazelcastInstance().getQueue(queueName);
            result.put(queueName, new QueueStatus(queue.size()));
        }
        return result;
    }
}
