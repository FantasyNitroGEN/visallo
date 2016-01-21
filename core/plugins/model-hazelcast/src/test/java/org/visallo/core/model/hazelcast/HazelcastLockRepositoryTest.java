package org.visallo.core.model.hazelcast;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.model.lock.LockRepositoryTestBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class HazelcastLockRepositoryTest extends LockRepositoryTestBase {
    private HazelcastLockRepository lockRepository;
    private HazelcastRepository hazelcastRepository;

    @Before
    public void before() {
        Map configurationMap = new HashMap();
        Configuration configuration = new HashMapConfigurationLoader(configurationMap).createConfiguration();
        String hazelcastConfigPath = HazelcastLockRepository.class.getResource("/org/visallo/core/model/hazelcast/hazelcast-config.xml").getFile();
        configuration.set(HazelcastConfiguration.CONFIGURATION_PREFIX + ".configFilePath", hazelcastConfigPath);
        hazelcastRepository = new HazelcastRepository(configuration);
        lockRepository = new HazelcastLockRepository(hazelcastRepository);
    }

    @After
    public void after() {
        lockRepository.shutdown();
        hazelcastRepository.getHazelcastInstance().shutdown();
    }

    @Test
    public void testCreateLock() throws Exception {
        super.testCreateLock(lockRepository);
    }

    @Test
    public void testLeaderElection() throws Exception {
        List<String> messages = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            threads.add(createLeaderElectingThread(lockRepository, "leaderOne", i, messages));
        }
        for (int i = 2; i < 5; i++) {
            threads.add(createLeaderElectingThread(lockRepository, "leaderTwo", i, messages));
        }
        for (Thread t : threads) {
            t.start();
        }
        for (int i = 0; i < 30 && messages.size() < 2; i++) {
            Thread.sleep(1000);
        }
        assertEquals(2, messages.size());
    }
}