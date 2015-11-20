package org.visallo.core.model.user;

import com.google.common.collect.HashBasedTable;
import com.google.inject.Inject;
import org.visallo.core.time.TimeRepository;

import java.util.Date;
import java.util.Map;

public class InMemoryUserSessionCounterRepository implements UserSessionCounterRepository {
    public final static int UNSEEN_SESSION_DURATION = 300000; // 5 minutes
    private final TimeRepository timeRepository;
    private final HashBasedTable<String, String, SessionData> sessionDatas = HashBasedTable.create();

    @Inject
    public InMemoryUserSessionCounterRepository(TimeRepository timeRepository) {
        this.timeRepository = timeRepository;
    }

    @Override
    public int updateSession(String userId, String sessionId, boolean autoDelete) {
        sessionDatas.put(userId, sessionId, new SessionData(timeRepository.getNow(), autoDelete));
        return getSessionCount(userId);
    }

    @Override
    public int getSessionCount(String userId) {
        for (Map.Entry<String, SessionData> sessionIdToSessionData : sessionDatas.row(userId).entrySet()) {
            if (shouldDelete(sessionIdToSessionData.getValue())) {
                sessionDatas.remove(userId, sessionIdToSessionData.getKey());
            }
        }
        return sessionDatas.row(userId).size();
    }

    private boolean shouldDelete(SessionData sessionData) {
        if (!sessionData.isAutoDelete()) {
            return false;
        }

        long now = timeRepository.currentTimeMillis();
        if (now - sessionData.getCreateDate().getTime() < UNSEEN_SESSION_DURATION) {
            return false;
        }

        return true;
    }

    @Override
    public void deleteSessions(String userId) {
        Map<String, SessionData> sessions = sessionDatas.row(userId);
        for (String sessionId : sessions.keySet()) {
            sessionDatas.remove(userId, sessionId);
        }
    }

    @Override
    public int deleteSession(String userId, String sessionId) {
        sessionDatas.remove(userId, sessionId);
        return getSessionCount(userId);
    }

    private class SessionData {
        private final Date createDate;
        private final boolean autoDelete;

        public SessionData(Date createDate, boolean autoDelete) {
            this.createDate = createDate;
            this.autoDelete = autoDelete;
        }

        public Date getCreateDate() {
            return createDate;
        }

        public boolean isAutoDelete() {
            return autoDelete;
        }

        @Override
        public String toString() {
            return "SessionData{" +
                    "createDate=" + createDate +
                    ", autoDelete=" + autoDelete +
                    '}';
        }
    }
}
