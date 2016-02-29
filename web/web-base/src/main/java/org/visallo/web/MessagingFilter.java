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
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class MessagingFilter implements PerRequestBroadcastFilter {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(MessagingFilter.class);
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

            String type = json.optString("type", null);
            if (type != null) {
                switch (type) {
                    case "setActiveWorkspace":
                        return new BroadcastAction(BroadcastAction.ACTION.ABORT, message);
                }
            }

            JSONObject permissionsJson = json.optJSONObject("permissions");
            if (permissionsJson != null) {
                JSONArray users = permissionsJson.optJSONArray("users");
                if (users != null) {
                    String currentUserId = CurrentUser.getUserId(r.getRequest().getSession());
                    if (currentUserId != null && !JSONUtil.isInArray(users, currentUserId)) {
                        return new BroadcastAction(BroadcastAction.ACTION.ABORT, message);
                    }
                }

                JSONArray sessionIds = permissionsJson.optJSONArray("sessionIds");
                if (sessionIds != null) {
                    String currentSessionId = r.getRequest().getSession().getId();
                    if (!JSONUtil.isInArray(sessionIds, currentSessionId)) {
                        return new BroadcastAction(BroadcastAction.ACTION.ABORT, message);
                    }
                }

                JSONArray workspaces = permissionsJson.optJSONArray("workspaces");
                if (workspaces != null) {
                    String currentUserId = CurrentUser.getUserId(r.getRequest().getSession());
                    if (!JSONUtil.isInArray(workspaces, userRepository.getCurrentWorkspaceId(currentUserId))) {
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
