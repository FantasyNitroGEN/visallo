package org.visallo.core.model.hazelcast;

import com.google.inject.Inject;
import com.hazelcast.core.IMap;
import org.visallo.core.config.Configuration;
import org.visallo.core.status.StatusData;
import org.visallo.core.status.StatusRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class HazelcastStatusRepository implements StatusRepository {
    private static final char KEY_SEPARATOR = (char) 0x1f;
    private final IMap<String, StatusData> map;
    private Configuration config;
    private int refreshAggregatesInterval;

    @Inject
    public HazelcastStatusRepository(
            HazelcastRepository hazelcastRepository,
            Configuration config
    ) {
        String statusMapName = hazelcastRepository.getHazelcastConfiguration().getStatusMapName();
        this.map = hazelcastRepository.getHazelcastInstance().getMap(statusMapName);
        this.config = config;
        refreshAggregatesInterval = config.getInt(Configuration.STATUS_REFRESH_INTERVAL_SECONDS, Configuration.STATUS_REFRESH_INTERVAL_SECONDS_DEFAULT);

    }

    @Override
    public StatusHandle saveStatus(String group, String instance, StatusData statusData) {
        map.put(getKey(group, instance), statusData, refreshAggregatesInterval + 1, TimeUnit.SECONDS);
        return new StatusHandle(group, instance);
    }

    @Override
    public void deleteStatus(StatusHandle statusHandle) {
        map.remove(getKey(statusHandle.getGroup(), statusHandle.getInstance()));
    }

    @Override
    public Iterable<String> getGroups() {
        Set<String> groups = new HashSet<>();
        for (String key : map.keySet()) {
            int token = key.indexOf(KEY_SEPARATOR);
            String group = key.substring(0, token);
            groups.add(group);
        }
        return groups;
    }

    @Override
    public Iterable<String> getInstances(String group) {
        List<String> instances = new ArrayList<>();
        String keyPrefix = group + KEY_SEPARATOR;
        for (String key : map.keySet()) {
            if (key.startsWith(keyPrefix)) {
                instances.add(key.substring(keyPrefix.length()));
            }
        }
        return instances;
    }

    @Override
    public StatusData getStatusData(String group, String instance) {
        return map.get(getKey(group, instance));
    }

    private String getKey(String group, String instance) {
        return group + KEY_SEPARATOR + instance;
    }
}
