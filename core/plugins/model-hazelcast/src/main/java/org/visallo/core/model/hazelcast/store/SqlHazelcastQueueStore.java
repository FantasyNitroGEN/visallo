package org.visallo.core.model.hazelcast.store;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hazelcast.core.QueueStore;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.hazelcast.HazelcastConfiguration;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class SqlHazelcastQueueStore extends SqlHazelcastStoreBase<Long, byte[]> implements QueueStore<byte[]> {
    @Inject
    public SqlHazelcastQueueStore(String name, HazelcastConfiguration hazelcastConfiguration) {
        super(name, StoreType.QUEUE, hazelcastConfiguration);
    }

    @Override
    public void store(Long key, byte[] value) {
        super.store(key, value);
    }

    @Override
    public void storeAll(Map<Long, byte[]> map) {
        super.storeAll(map);
    }

    @Override
    public void delete(Long key) {
        super.delete(key);
    }

    @Override
    public void deleteAll(Collection<Long> keys) {
        super.deleteAll(keys);
    }

    @Override
    public byte[] load(Long key) {
        return super.load(key);
    }

    @Override
    public Map<Long, byte[]> loadAll(Collection<Long> keys) {
        return super.loadAll(keys);
    }

    @Override
    public Set<Long> loadAllKeys() {
        return Sets.newHashSet(super.loadAllKeysIterable());
    }

    @Override
    protected Long getKeyFromQueryRow(Map<String, Object> row) {
        Object obj = getKeyObjectFromQueryRow(row);
        if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Integer) {
            int i = (int) obj;
            return (long) i;
        } else {
            throw new VisalloException("Could not handle key type: " + obj.getClass().getName());
        }
    }

    @Override
    protected byte[] deserializeValue(byte[] bytes) {
        return bytes;
    }
}
