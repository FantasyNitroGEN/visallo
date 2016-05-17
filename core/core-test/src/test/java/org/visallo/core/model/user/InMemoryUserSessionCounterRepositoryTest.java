package org.visallo.core.model.user;

import org.junit.Before;
import org.junit.Test;
import org.visallo.core.time.MockTimeRepository;

import java.util.Date;

import static junit.framework.TestCase.assertEquals;

public class InMemoryUserSessionCounterRepositoryTest {
    public static final String USER1_ID = "user1";
    private MockTimeRepository timeRepository = new MockTimeRepository();
    private InMemoryUserSessionCounterRepository sessionCounterRepository;

    @Before
    public void setUp() {
        timeRepository.setNow(new Date());
        sessionCounterRepository = new InMemoryUserSessionCounterRepository(timeRepository);
    }

    @Test
    public void testDeleteSessionsNoCurrentSessions() {
        assertEquals(0, sessionCounterRepository.getSessionCount(USER1_ID));
        sessionCounterRepository.deleteSessions(USER1_ID);
        assertEquals(0, sessionCounterRepository.getSessionCount(USER1_ID));
    }

    @Test
    public void testDeleteSessionsWithCurrentSessions() {
        assertEquals(0, sessionCounterRepository.getSessionCount(USER1_ID));
        assertEquals(1, sessionCounterRepository.updateSession(USER1_ID, "session1", false));
        assertEquals(1, sessionCounterRepository.getSessionCount(USER1_ID));
        sessionCounterRepository.deleteSessions(USER1_ID);
        assertEquals(0, sessionCounterRepository.getSessionCount(USER1_ID));
    }

    @Test
    public void testUpdateSession() {
        assertEquals(0, sessionCounterRepository.getSessionCount(USER1_ID));
        assertEquals(1, sessionCounterRepository.updateSession(USER1_ID, "session1", false));
        assertEquals(2, sessionCounterRepository.updateSession(USER1_ID, "session2", false));
        assertEquals(2, sessionCounterRepository.updateSession(USER1_ID, "session2", false));
        assertEquals(2, sessionCounterRepository.getSessionCount(USER1_ID));
    }

    @Test
    public void testDeleteSession() {
        assertEquals(0, sessionCounterRepository.getSessionCount(USER1_ID));
        assertEquals(1, sessionCounterRepository.updateSession(USER1_ID, "session1", false));
        assertEquals(2, sessionCounterRepository.updateSession(USER1_ID, "session2", false));
        assertEquals(1, sessionCounterRepository.deleteSession(USER1_ID, "session2"));
        assertEquals(0, sessionCounterRepository.deleteSession(USER1_ID, "session1"));
        assertEquals(0, sessionCounterRepository.getSessionCount(USER1_ID));
    }

    @Test
    public void testExpire() {
        Date t1 = new Date(System.currentTimeMillis());
        Date t2 = new Date(System.currentTimeMillis() + 5000);
        Date t3 = new Date(System.currentTimeMillis() + InMemoryUserSessionCounterRepository.UNSEEN_SESSION_DURATION + 100);

        assertEquals(0, sessionCounterRepository.getSessionCount(USER1_ID));
        timeRepository.setNow(t1);
        assertEquals(1, sessionCounterRepository.updateSession(USER1_ID, "session1", true));
        assertEquals(1, sessionCounterRepository.getSessionCount(USER1_ID));
        timeRepository.setNow(t2);
        assertEquals(1, sessionCounterRepository.getSessionCount(USER1_ID));
        timeRepository.setNow(t3);
        assertEquals(0, sessionCounterRepository.getSessionCount(USER1_ID));
    }
}