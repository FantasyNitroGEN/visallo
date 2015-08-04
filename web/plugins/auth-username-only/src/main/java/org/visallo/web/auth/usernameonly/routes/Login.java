package org.visallo.web.auth.usernameonly.routes;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.utils.UrlUtils;
import org.json.JSONObject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.AuthenticationHandler;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.CurrentUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Login extends BaseRequestHandler {
    @Inject
    public Login(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration
    ) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String username = UrlUtils.urlDecode(request.getParameter("username")).trim().toLowerCase();

        User user = getUserRepository().findByUsername(username);
        if (user == null) {
            // For form based authentication, username and displayName will be the same
            String randomPassword = UserRepository.createRandomPassword();
            user = getUserRepository().findOrAddUser(username, username, null, randomPassword, new String[0]);
        }

        getUserRepository().recordLogin(user, AuthenticationHandler.getRemoteAddr(request));

        CurrentUser.set(request, user.getUserId(), user.getUsername());
        JSONObject json = new JSONObject();
        json.put("status", "OK");
        respondWithJson(response, json);
    }
}
