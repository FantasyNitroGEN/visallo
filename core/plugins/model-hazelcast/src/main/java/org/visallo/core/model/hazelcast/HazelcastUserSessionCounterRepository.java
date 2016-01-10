package org.visallo.core.model.hazelcast;

import com.google.inject.Inject;
import com.hazelcast.core.IMap;
import org.visallo.core.model.user.MapUserSessionCounterRepositoryBase;
import org.visallo.core.time.TimeRepository;

import java.util.HashMap;
import java.util.Map;

public class HazelcastUserSessionCounterRepository extends MapUserSessionCounterRepositoryBase {
    private static final char KEY_SEPARATOR = (char) 0x1f;
    private final IMap<String, SessionData> map;

    @Inject
    public HazelcastUserSessionCounterRepository(
            TimeRepository timeRepository,
            HazelcastRepository hazelcastRepository
    ) {
        super(timeRepository);
        String userSessionCounterMapName = hazelcastRepository.getHazelcastConfiguration().getUserSessionCounterMapName();
        this.map = hazelcastRepository.getHazelcastInstance().getMap(userSessionCounterMapName);
    }

    @Override
    protected void put(String userId, String sessionId, SessionData sessionData) {
        this.map.put(getKey(userId, sessionId), sessionData);
    }

    @Override
    protected void remove(String userId, String sessionId) {
        this.map.remove(getKey(userId, sessionId));
    }

    @Override
    protected Map<String, SessionData> getRow(String userId) {
        String keyPrefix = userId + KEY_SEPARATOR;
        IMap<String, SessionData> map = this.map;
        Map<String, SessionData> result = new HashMap<>();
        for (String key : map.keySet()) {
            if (key.startsWith(keyPrefix)) {
                result.put(key.substring(keyPrefix.length()), map.get(key));
            }
        }
        return result;
    }

    private String getKey(String userId, String sessionId) {
        return userId + KEY_SEPARATOR + sessionId;
    }
}
