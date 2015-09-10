package org.visallo.model.rabbitmq;

import com.google.inject.Inject;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.externalResource.ExternalResourceWorker;
import org.visallo.core.externalResource.QueueExternalResourceWorker;
import org.visallo.core.ingest.WorkerSpout;
import org.visallo.core.model.FlushFlag;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.IOException;
import java.util.*;

public class RabbitMQWorkQueueRepository extends WorkQueueRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(RabbitMQWorkQueueRepository.class);
    private static final String DEFAULT_BROADCAST_EXCHANGE_NAME = "exBroadcast";

    private final Connection connection;
    private final Channel channel;
    private Set<String> declaredQueues = new HashSet<>();

    @Inject
    public RabbitMQWorkQueueRepository(Graph graph, WorkQueueNames workQueueNames, Configuration configuration)
            throws IOException {
        super(graph, workQueueNames, configuration);
        this.connection = RabbitMQUtils.openConnection(configuration);
        this.channel = RabbitMQUtils.openChannel(this.connection);
        this.channel.exchangeDeclare(getExchangeName(), "fanout");
    }

    @Override
    protected void broadcastJson(JSONObject json) {
        try {
            LOGGER.debug("publishing message to broadcast exchange [%s]: %s", getExchangeName(), json.toString());
            channel.basicPublish(getExchangeName(), "", null, json.toString().getBytes());
        } catch (IOException ex) {
            throw new VisalloException("Could not broadcast json", ex);
        }
    }

    @Override
    public void pushOnQueue(String queueName, FlushFlag flushFlag, JSONObject json, Priority priority) {
        try {
            ensureQueue(queueName);
            json.put("priority", priority.name());
            LOGGER.debug("enqueuing message to queue [%s]: %s", queueName, json.toString());
            AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
            propsBuilder.priority(toRabbitMQPriority(priority));
            channel.basicPublish("", queueName, propsBuilder.build(), json.toString().getBytes());
        } catch (Exception ex) {
            throw new VisalloException("Could not push on queue", ex);
        }
    }

    private Integer toRabbitMQPriority(Priority priority) {
        switch (priority) {
            case HIGH:
                return 2;
            case NORMAL:
                return 1;
            case LOW:
                return 0;
            default:
                return 0;
        }
    }

    private void ensureQueue(String queueName) throws IOException {
        if (!declaredQueues.contains(queueName)) {
            createQueue(channel, queueName);
            declaredQueues.add(queueName);
        }
    }

    public static void createQueue(Channel channel, String queueName) throws IOException {
        Map<String, Object> args = new HashMap<>();
        args.put("x-max-priority", 3);
        channel.queueDeclare(queueName, true, false, false, args);
    }

    @Override
    public void flush() {
    }

    @Override
    public void shutdown() {
        super.shutdown();
        try {
            LOGGER.debug("Closing RabbitMQ channel");
            this.channel.close();
        } catch (Throwable e) {
            LOGGER.error("Could not close RabbitMQ channel", e);
        }
        try {
            LOGGER.debug("Closing RabbitMQ connection");
            this.connection.close();
        } catch (Throwable e) {
            LOGGER.error("Could not close RabbitMQ connection", e);
        }
    }

    @Override
    public void format() {
        try {
            LOGGER.info("deleting queue: %s", workQueueNames.getGraphPropertyQueueName());
            LOGGER.info("deleting queue: %s", workQueueNames.getLongRunningProcessQueueName());
            channel.queueDelete(workQueueNames.getGraphPropertyQueueName());
            channel.queueDelete(workQueueNames.getLongRunningProcessQueueName());

            Collection<ExternalResourceWorker> externalResourceWorkers =
                    InjectHelper.getInjectedServices(ExternalResourceWorker.class, configuration);
            for (ExternalResourceWorker externalResourceWorker : externalResourceWorkers) {
                if (!(externalResourceWorker instanceof QueueExternalResourceWorker)) {
                    continue;
                }
                String queueName = ((QueueExternalResourceWorker) externalResourceWorker).getQueueName();
                channel.queueDelete(queueName);
            }
        } catch (IOException e) {
            throw new VisalloException("Could not delete queues", e);
        }
    }

    @Override
    public void subscribeToBroadcastMessages(final BroadcastConsumer broadcastConsumer) {
        try {
            String queueName = this.channel.queueDeclare().getQueue();
            this.channel.queueBind(queueName, getExchangeName(), "");

            final QueueingConsumer callback = new QueueingConsumer(this.channel);
            this.channel.basicConsume(queueName, true, callback);

            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //noinspection InfiniteLoopStatement
                        while (true) {
                            QueueingConsumer.Delivery delivery = callback.nextDelivery();
                            try {
                                JSONObject json = new JSONObject(new String(delivery.getBody()));
                                LOGGER.debug("received message from broadcast exchange [%s]: %s", getExchangeName(), json.toString());
                                broadcastConsumer.broadcastReceived(json);
                            } catch (Throwable ex) {
                                LOGGER.error("problem in broadcast thread", ex);
                            }
                        }
                    } catch (InterruptedException e) {
                        throw new VisalloException("broadcast listener has died", e);
                    }
                }
            });
            t.setName("rabbitmq-subscribe-" + broadcastConsumer.getClass().getName());
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            throw new VisalloException("Could not subscribe to broadcasts", e);
        }
    }

    @Override
    public WorkerSpout createWorkerSpout(String queueName) {
        return InjectHelper.inject(new RabbitMQWorkQueueSpout(queueName));
    }

    private String getExchangeName(){
        return this.configuration.get(Configuration.BROADCAST_EXCHANGE_NAME_CONFIGURATION, DEFAULT_BROADCAST_EXCHANGE_NAME);
    }
}
