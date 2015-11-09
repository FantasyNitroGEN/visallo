package org.visallo.model.rabbitmq;

import com.beust.jcommander.internal.Lists;
import com.rabbitmq.client.*;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class RabbitMQUtils {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(RabbitMQUtils.class);
    public static final String RABBITMQ_ADDR_PREFIX = "rabbitmq.addr";
    public static final String RABBITMQ_DELIVERY_MODE = "rabbitmq.deliveryMode";
    private static final int DEFAULT_PORT = 5672;

    public static Connection openConnection(String[] addresses) throws IOException {
        Address[] addressesArr = createAddresses(addresses);
        return openConnection(addressesArr);
    }

    public static Connection openConnection(Address[] addresses) throws IOException {
        if (addresses.length == 0) {
            throw new VisalloException("Could not configure RabbitMQ. No addresses specified. expecting configuration parameter 'rabbitmq.addr.0.host'.");
        }

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setAutomaticRecoveryEnabled(true);
        final Connection connection = connectionFactory.newConnection(addresses);
        ((Recoverable) connection).addRecoveryListener(new RecoveryListener() {
            @Override
            public void handleRecovery(Recoverable recoverable) {
                Connection recoveredConnection = (Connection) recoverable;
                LOGGER.warn("recovered RabbitMQ connection to %s:%d", recoveredConnection.getAddress(), recoveredConnection.getPort());
            }
        });
        return connection;
    }

    public static Connection openConnection(Configuration configuration) throws IOException {
        Address[] addresses = getAddresses(configuration);
        return openConnection(addresses);
    }

    public static Channel openChannel(Connection connection) {
        try {
            return connection.createChannel();
        } catch (IOException ex) {
            throw new VisalloException("Could not open channel to RabbitMQ", ex);
        }
    }

    public static Address[] getAddresses(Configuration configuration) {
        List<Address> addresses = new ArrayList<Address>();
        for (String key : configuration.getKeys(RABBITMQ_ADDR_PREFIX)) {
            if (key.endsWith(".host")) {
                String host = configuration.get(key, null);
                checkNotNull(host, "Configuration " + key + " is required");
                int port = configuration.getInt(key.replace(".host", ".port"), DEFAULT_PORT);
                addresses.add(new Address(host, port));
            }
        }
        return addresses.toArray(new Address[addresses.size()]);
    }

    private static Address[] createAddresses(String[] addresses) {
        List<Address> addressList = Lists.newArrayList();

        for (String address : addresses) {
            String[] addressParts = address.split(":");

            if (addressParts.length == 1) {
                addressList.add(new Address(address));
            } else if (addressParts.length == 2) {
                addressList.add(new Address(addressParts[0], Integer.parseInt(addressParts[1])));
            } else {
                throw new IllegalArgumentException(String.format("malformed rabbitmq address: %s", address));
            }
        }

        return addressList.toArray(new Address[0]);
    }
}
