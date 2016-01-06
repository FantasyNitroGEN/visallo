package org.visallo.core.model.hazelcast.store;

import com.hazelcast.core.MapStore;
import com.hazelcast.core.MapStoreFactory;
import com.hazelcast.core.QueueStore;
import com.hazelcast.core.QueueStoreFactory;
import org.visallo.core.model.hazelcast.HazelcastConfiguration;

import java.util.Properties;

public class SqlHazelcastStoreFactory implements QueueStoreFactory<byte[]>, MapStoreFactory<String, Object> {
    private final HazelcastConfiguration hazelcastConfiguration;

    public SqlHazelcastStoreFactory(HazelcastConfiguration hazelcastConfiguration) {
        this.hazelcastConfiguration = hazelcastConfiguration;
    }

    @Override
    public QueueStore<byte[]> newQueueStore(String name, Properties properties) {
        return new SqlHazelcastQueueStore(name, hazelcastConfiguration);
    }

    @Override
    public MapStore<String, Object> newMapStore(String mapName, Properties properties) {
        return new SqlHazelcastMapStore(mapName, hazelcastConfiguration);
    }
}
