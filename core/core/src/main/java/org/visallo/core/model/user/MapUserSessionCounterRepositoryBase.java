package org.visallo.core.model.user;

import org.visallo.core.time.TimeRepository;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public abstract class MapUserSessionCounterRepositoryBase implements UserSessionCounterRepository {
    public final static int UNSEEN_SESSION_DURATION = 300000; // 5 minutes
    private final TimeRepository timeRepository;

    protected MapUserSessionCounterRepositoryBase(TimeRepository timeRepository) {
        this.timeRepository = timeRepository;
    }

    @Override
    public int updateSession(String userId, String sessionId, boolean autoDelete) {
        put(userId, sessionId, new SessionData(timeRepository.getNow(), autoDelete));
        return getSessionCount(userId);
    }

    protected abstract void put(String userId, String sessionId, SessionData sessionData);

    protected abstract void remove(String userId, String sessionId);

    protected abstract Map<String, SessionData> getRow(String userId);

    @Override
    public int getSessionCount(String userId) {
        for (Map.Entry<String, SessionData> sessionIdToSessionData : getRow(userId).entrySet()) {
            if (shouldDelete(sessionIdToSessionData.getValue())) {
                remove(userId, sessionIdToSessionData.getKey());
            }
        }
        return getRow(userId).size();
    }

    private boolean shouldDelete(SessionData sessionData) {
        if (!sessionData.isAutoDelete()) {
            return false;
        }

        long now = timeRepository.currentTimeMillis();
        return now - sessionData.getCreateDate().getTime() >= UNSEEN_SESSION_DURATION;

    }

    @Override
    public void deleteSessions(String userId) {
        Map<String, SessionData> sessions = getRow(userId);
        for (String sessionId : sessions.keySet()) {
            remove(userId, sessionId);
        }
    }

    @Override
    public int deleteSession(String userId, String sessionId) {
        remove(userId, sessionId);
        return getSessionCount(userId);
    }

    protected static class SessionData implements Serializable {
        private static final long serialVersionUID = -1883352978079887306L;
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
