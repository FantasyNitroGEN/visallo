package org.visallo.web;

import com.google.inject.Inject;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.PerRequestBroadcastFilter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.util.JSONUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.servlet.http.HttpSession;

import static com.google.common.base.Preconditions.checkNotNull;

public class MessagingFilter implements PerRequestBroadcastFilter {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(MessagingFilter.class);
    public static final String TYPE_SESSION_EXPIRATION = "sessionExpiration";
    public static final String TYPE_SET_ACTIVE_WORKSPACE = "setActiveWorkspace";
    private UserRepository userRepository;

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

            if (shouldSendMessage(json, r.getRequest().getSession())) {
                return new BroadcastAction(message);
            } else {
                return new BroadcastAction(BroadcastAction.ACTION.ABORT, message);
            }
        } catch (JSONException e) {
            LOGGER.error("Failed to filter message:\n" + originalMessage, e);
            return new BroadcastAction(BroadcastAction.ACTION.ABORT, message);
        }
    }

    boolean shouldSendMessage(JSONObject json, HttpSession session) {
        String type = json.optString("type", null);
        if (type != null) {
            switch (type) {
                case TYPE_SET_ACTIVE_WORKSPACE:
                    return false;
                case TYPE_SESSION_EXPIRATION:
                    if (session == null) {
                        return true;
                    }
                    break;
            }
        }

        if (session == null) {
            return false;
        }

        return shouldSendMessageByPermissions(json, session);
    }

    private boolean shouldSendMessageByPermissions(JSONObject json, HttpSession session) {
        JSONObject permissionsJson = json.optJSONObject("permissions");
        if (permissionsJson != null) {
            if (shouldRejectMessageByUsers(permissionsJson, session)) {
                return false;
            }

            if (shouldRejectMessageToSessionIds(permissionsJson, session)) {
                return false;
            }

            if (shouldRejectMessageToWorkspaces(permissionsJson, session)) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldRejectMessageToWorkspaces(JSONObject permissionsJson, HttpSession session) {
        JSONArray workspaces = permissionsJson.optJSONArray("workspaces");
        if (workspaces != null) {
            String currentUserId = CurrentUser.getUserId(session);
            if (currentUserId == null) {
                return true;
            }

            String currentWorkspaceId = userRepository.getCurrentWorkspaceId(currentUserId);
            if (currentWorkspaceId == null) {
                return true;
            }

            if (!JSONUtil.isInArray(workspaces, currentWorkspaceId)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldRejectMessageToSessionIds(JSONObject permissionsJson, HttpSession session) {
        JSONArray sessionIds = permissionsJson.optJSONArray("sessionIds");
        if (sessionIds != null) {
            if (!JSONUtil.isInArray(sessionIds, session.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldRejectMessageByUsers(JSONObject permissionsJson, HttpSession session) {
        JSONArray users = permissionsJson.optJSONArray("users");
        if (users != null) {
            String currentUserId = CurrentUser.getUserId(session);
            if (currentUserId != null && !JSONUtil.isInArray(users, currentUserId)) {
                return true;
            }
        }
        return false;
    }

    public void ensureInitialized() {
        if (userRepository == null) {
            InjectHelper.inject(this);
            if (userRepository == null) {
                LOGGER.error("userRepository cannot be null");
                checkNotNull(userRepository, "userRepository cannot be null");
            }
        }
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

}
