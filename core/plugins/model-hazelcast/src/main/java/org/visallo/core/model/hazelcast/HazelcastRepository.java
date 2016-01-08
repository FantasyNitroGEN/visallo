package org.visallo.core.model.hazelcast;

import com.google.inject.Inject;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.visallo.core.config.Configuration;

public class HazelcastRepository {
    private final HazelcastConfiguration hazelcastConfiguration;

    @Inject
    public HazelcastRepository(Configuration configuration) {
        hazelcastConfiguration = HazelcastConfiguration.create(configuration);
    }

    public HazelcastInstance getHazelcastInstance() {
        Config config = hazelcastConfiguration.getConfig();
        return Hazelcast.getOrCreateHazelcastInstance(config);
    }

    public HazelcastConfiguration getHazelcastConfiguration() {
        return this.hazelcastConfiguration;
    }
}
