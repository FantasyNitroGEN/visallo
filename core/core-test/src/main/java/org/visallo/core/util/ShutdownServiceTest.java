package org.visallo.core.util;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ShutdownServiceTest {
    private ShutdownService shutdownService;
    private List<Class> shutdownList;

    @Before
    public void before() {
        shutdownService = new ShutdownService();
        shutdownList = new ArrayList<>();
    }

    @Test
    public void testShutdown() {
        A a = new A();
        B b = new B(a);
        shutdownService.register(a);
        shutdownService.register(b);

        shutdownService.shutdown();

        assertEquals(2, shutdownList.size());

        // B should shutdown first because B depends on A
        assertEquals(B.class, shutdownList.get(0));
        assertEquals(A.class, shutdownList.get(1));
    }

    private class A implements ShutdownListener {
        public boolean hasShutdown = false;

        public A() {
            shutdownService.register(this);
        }

        @Override
        public void shutdown() {
            hasShutdown = true;
            shutdownList.add(A.class);
        }
    }

    private class B implements ShutdownListener {
        private final A a;

        public B(A a) {
            this.a = a;
            shutdownService.register(this);
        }

        @Override
        public void shutdown() {
            assertFalse("A should shutdown after B", a.hasShutdown);
            shutdownList.add(B.class);
        }
    }
}