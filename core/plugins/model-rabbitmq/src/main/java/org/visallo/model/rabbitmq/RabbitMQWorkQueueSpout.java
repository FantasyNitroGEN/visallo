package org.visallo.model.rabbitmq;

import com.google.inject.Inject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.WorkerSpout;
import org.visallo.core.ingest.WorkerTuple;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.json.JSONObject;

import java.io.IOException;

public class RabbitMQWorkQueueSpout extends WorkerSpout {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(RabbitMQWorkQueueSpout.class);
    public static final int DEFAULT_RABBITMQ_PREFETCH_COUNT = 10;
    private final String queueName;
    private Channel channel;
    private QueueingConsumer consumer;
    private Connection connection;
    private Configuration configuration;

    public RabbitMQWorkQueueSpout(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public void open() {
        try {
            this.connection = RabbitMQUtils.openConnection(configuration);
            this.channel = RabbitMQUtils.openChannel(this.connection);
            RabbitMQWorkQueueRepository.createQueue(channel, queueName);
            this.consumer = new QueueingConsumer(channel);
            Integer prefetchCount = configuration.getInt(Configuration.RABBITMQ_PREFETCH_COUNT, DEFAULT_RABBITMQ_PREFETCH_COUNT);
            this.channel.basicQos(prefetchCount, false);
            this.channel.basicConsume(this.queueName, false, consumer);
        } catch (IOException ex) {
            throw new VisalloException("Could not startup RabbitMQ", ex);
        }
    }

    @Override
    public void close() {
        super.close();
        try {
            LOGGER.debug("Closing RabbitMQ channel");
            this.channel.close();
            LOGGER.debug("Closing RabbitMQ connection");
            this.connection.close();
        } catch (IOException ex) {
            LOGGER.error("Could not close RabbitMQ connection and channel", ex);
        }
    }

    @Override
    public WorkerTuple nextTuple() throws InterruptedException {
        QueueingConsumer.Delivery delivery = this.consumer.nextDelivery(100);
        if (delivery == null) {
            return null;
        }
        JSONObject json = new JSONObject(new String(delivery.getBody()));
        LOGGER.debug("emit (%s): %s", this.queueName, json.toString());
        return new WorkerTuple(delivery.getEnvelope().getDeliveryTag(), json);
    }

    @Override
    public void ack(Object msgId) {
        super.ack(msgId);
        long deliveryTag = (Long) msgId;
        try {
            this.channel.basicAck(deliveryTag, false);
        } catch (IOException ex) {
            LOGGER.error("Could not ack: %d", deliveryTag, ex);
        }
    }

    @Override
    public void fail(Object msgId) {
        super.fail(msgId);
        long deliveryTag = (Long) msgId;
        try {
            this.channel.basicNack(deliveryTag, false, false);
        } catch (IOException ex) {
            LOGGER.error("Could not ack: %d", deliveryTag, ex);
        }
    }

    protected QueueingConsumer getConsumer() {
        return this.consumer;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
