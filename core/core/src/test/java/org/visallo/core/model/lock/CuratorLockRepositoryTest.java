package org.visallo.core.model.lock;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.HashMapConfigurationLoader;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CuratorLockRepositoryTest extends LockRepositoryTestBase {
    private static TestingServer zk;
    private static CuratorFramework curatorFramework;
    private static LockRepository lockRepository;

    @BeforeClass
    public static void setup() throws Exception {
        zk = new TestingServer();
        zk.start();
        curatorFramework = CuratorFrameworkFactory.newClient(zk.getConnectString(), new ExponentialBackoffRetry(1000, 6));
        curatorFramework.start();

        Configuration configuration = new HashMapConfigurationLoader(new HashMap()).createConfiguration();
        lockRepository = new CuratorLockRepository(curatorFramework, configuration);
    }

    @AfterClass
    public static void teardown() throws IOException {
        curatorFramework.close();
        zk.stop();
    }

    @Test
    public void testCreateLock() throws Exception {
        List<String> messages = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            threads.add(createLockExercisingThread(lockRepository, "lockOne", i, messages));
        }
        for (int i = 5; i < 10; i++) {
            threads.add(createLockExercisingThread(lockRepository, "lockTwo", i, messages));
        }
        for (Thread t : threads) {
            t.start();
            t.join();
        }
        assertEquals(threads.size(), messages.size());
    }

    @Test
    @Ignore("leaderOne is broken, only a leaderTwo is elected")
    public void testLeaderElection() throws Exception {
        List<String> messages = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        System.out.println("thread: "+Thread.currentThread().getName());
        for (int i = 0; i < 2; i++) {
            threads.add(createLeaderElectingThread(lockRepository, "leaderOne", i, messages));
        }
        for (int i = 2; i < 5; i++) {
            threads.add(createLeaderElectingThread(lockRepository, "leaderTwo", i, messages));
        }
        for (Thread t : threads) {
            t.start();
        }
        Thread.sleep(1000);
        printZk();
        for (String message : messages) {
            System.out.println(message);
        }
        assertEquals(2, messages.size());
    }

    private void printZk() throws Exception {
        List<String> list = new ArrayList<>();
        list.add("/");
        printZk(list);
    }

    private void printZk(Iterable<String> paths) throws Exception {
        for (final String path : paths) {
            System.out.println(path);
            Iterable<String> children = curatorFramework.getChildren().forPath(path);
            children = Iterables.transform(children, new Function<String, String>() {
                @Nullable
                @Override
                public String apply(String child) {
                    return "/".equals(path) ? path + child : path + "/" + child;
                }
            });
            printZk(children);
        }
    }
}
