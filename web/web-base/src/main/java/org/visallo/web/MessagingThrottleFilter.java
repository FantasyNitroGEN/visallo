package org.visallo.web;

import com.google.inject.Inject;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.PerRequestBroadcastFilter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.util.JSONUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class MessagingThrottleFilter implements PerRequestBroadcastFilter {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(MessagingThrottleFilter.class);
    private UserRepository userRepository;
    private Integer throttleMillis;

    private final Map<String, Long> lastRequestForUuid = new ConcurrentHashMap<>();
    private final Map<String, List<JSONObject>> messagesForUuid = new ConcurrentHashMap<>();
    private final Map<String, Boolean> broadcastScheduledForUuid = new ConcurrentHashMap<>();

    @Override
    public BroadcastAction filter(String broadcasterId, Object originalMessage, Object message) {
        return new BroadcastAction(message);
    }

    @Override
    public BroadcastAction filter(String broadcasterId, AtmosphereResource r, Object originalMessage, Object message) {
        ensureInitialized();

        try {
            if (message == null || r.isCancelled()) {
                return new BroadcastAction(BroadcastAction.ACTION.ABORT, null);
            }
            JSONObject json = new JSONObject(message.toString());

            if (this.throttleMillis > 0 && !"batch".equals(json.optString("type"))) {
                String uuid = r.uuid();

                synchronized (getMutex(uuid)) {
                    boolean queueFuture = !broadcastScheduledForUuid.containsKey(uuid);
                    Long timeSinceLastRequest = getTimeSinceLastRequest(uuid);

                    if (timeSinceLastRequest < this.throttleMillis) {
                        addMessageToBatch(json, uuid);
                        if (queueFuture) {
                            delayBatchBroadcast(r, this.throttleMillis - timeSinceLastRequest);
                        }
                        return new BroadcastAction(BroadcastAction.ACTION.ABORT, message);
                    }
                }
            }

            return new BroadcastAction(message);
        } catch (JSONException e) {
            LOGGER.error("Failed to filter message:\n" + originalMessage, e);
            return new BroadcastAction(BroadcastAction.ACTION.ABORT, message);
        }
    }

    private void addMessageToBatch(JSONObject json, String uuid) {
        List<JSONObject> messages;
        if (!messagesForUuid.containsKey(uuid)) {
            messages = new ArrayList<>();
            messagesForUuid.put(uuid, messages);
        } else {
            messages = messagesForUuid.get(uuid);
        }

        boolean contains = messages.stream().anyMatch(jsonObject -> {
            return JSONUtil.areEqual(jsonObject, json);
        });

        if (!contains) {
            messages.add(json);
        }
    }

    private Long getTimeSinceLastRequest(String uuid) {
        Long lastRequest = 0L;
        Long now = new Date().getTime();
        if (lastRequestForUuid.containsKey(uuid)) {
            lastRequest = lastRequestForUuid.get(uuid);
        }
        lastRequestForUuid.put(uuid, now);

        return now - lastRequest;
    }

    private void delayBatchBroadcast(final AtmosphereResource r, Long delay) {
        String uuid = r.uuid();
        r.getBroadcaster().getBroadcasterConfig().getScheduledExecutorService().schedule(new Runnable() {
            @Override
            public void run() {
                synchronized (getMutex(uuid)) {
                    List<JSONObject> messages = messagesForUuid.get(uuid);
                    if (messages != null) {
                        JSONObject batchMessage = new JSONObject();
                        JSONArray jsonMessages = new JSONArray(messages);
                        batchMessage.put("data", jsonMessages);
                        batchMessage.put("type", "batch");

                        r.getBroadcaster().broadcast(batchMessage.toString(), r);

                        broadcastScheduledForUuid.remove(uuid);
                        messages.clear();
                    }
                }
            }
        }, delay, TimeUnit.MILLISECONDS);
        broadcastScheduledForUuid.put(uuid, true);
    }

    private String getMutex(String uuid) {
        return (this.getClass().getName() + uuid).intern();
    }

    public void ensureInitialized() {
        if (userRepository == null) {
            InjectHelper.inject(this);
            if (userRepository == null) {
                LOGGER.error("userRepository cannot be null");
                checkNotNull(userRepository, "userRepository cannot be null");
            }
            if (this.throttleMillis == null) {
                LOGGER.error("throttleMillis cannot be null");
                checkNotNull(throttleMillis, "throttleMillis cannot be null");
            }
        }
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.throttleMillis = configuration.getInt(WebConfiguration.THROTTLE_MESSAGING_SECONDS) * 1000;
    }
}
