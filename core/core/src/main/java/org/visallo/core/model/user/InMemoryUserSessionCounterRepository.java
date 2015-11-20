package org.visallo.core.model.user;

import com.google.common.collect.HashBasedTable;

import java.util.Date;
import java.util.Map;

public class InMemoryUserSessionCounterRepository implements UserSessionCounterRepository {
    protected final static int UNSEEN_SESSION_DURATION = 300000; // 5 minutes

    private HashBasedTable<String, String, SessionData> sessionDatas = HashBasedTable.create();

    @Override
    public int updateSession(String userId, String sessionId, boolean autoDelete) {
        sessionDatas.put(userId, sessionId, new SessionData(autoDelete));
        return getUsersSessionCount(userId);
    }

    private int getUsersSessionCount(String userId) {
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

        long now = System.currentTimeMillis();
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
        return getUsersSessionCount(userId);
    }

    private class SessionData {
        private final Date createDate = new Date();
        private final boolean autoDelete;

        public SessionData(boolean autoDelete) {
            this.autoDelete = autoDelete;
        }

        public Date getCreateDate() {
            return createDate;
        }

        public boolean isAutoDelete() {
            return autoDelete;
        }
    }
}
