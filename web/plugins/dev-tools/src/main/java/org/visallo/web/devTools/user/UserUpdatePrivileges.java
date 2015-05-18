package org.visallo.web.devTools.user;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.Privilege;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

public class UserUpdatePrivileges extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(UserUpdatePrivileges.class);

    @Inject
    public UserUpdatePrivileges(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration
    ) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String userName = getRequiredParameter(request, "user-name");
        User authUser = getUser(request);
        Set<Privilege> privileges = Privilege.stringToPrivileges(getRequiredParameter(request, "privileges"));

        User user = getUserRepository().findByUsername(userName);
        if (user == null) {
            respondWithNotFound(response);
            return;
        }

        LOGGER.info("Setting user %s privileges to %s", user.getUserId(), Privilege.toString(privileges));
        getUserRepository().setPrivileges(user, privileges, authUser);

        JSONObject json = getUserRepository().toJsonWithAuths(user);
        respondWithJson(response, json);
    }
}
