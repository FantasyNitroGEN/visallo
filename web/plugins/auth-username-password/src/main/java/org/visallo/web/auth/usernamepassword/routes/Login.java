package org.visallo.web.auth.usernamepassword.routes;

import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.utils.UrlUtils;
import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.AuthenticationHandler;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.CurrentUser;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Login extends BaseRequestHandler {

    @Inject
    public Login(UserRepository userRepository, WorkspaceRepository workspaceRepository, Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String username = UrlUtils.urlDecode(request.getParameter("username"));
        final String password = UrlUtils.urlDecode(request.getParameter("password")).trim();

        User user = getUserRepository().findByUsername(username);
        if (user != null && getUserRepository().isPasswordValid(user, password)) {
            getUserRepository().recordLogin(user, AuthenticationHandler.getRemoteAddr(request));
            CurrentUser.set(request, user.getUserId(), user.getUsername());
            JSONObject json = new JSONObject();
            json.put("status", "OK");
            respondWithJson(response, json);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }

    }
}
