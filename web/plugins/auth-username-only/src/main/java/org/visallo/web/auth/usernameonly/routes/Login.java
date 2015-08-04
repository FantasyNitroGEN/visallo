package org.visallo.web.auth.usernameonly.routes;

import com.google.inject.Inject;
import org.vertexium.Vertex;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.user.UserVisalloProperties;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.utils.UrlUtils;
import org.visallo.web.AuthenticationHandler;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.CurrentUser;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.Callable;

import static org.vertexium.util.IterableUtils.singleOrDefault;

public class Login extends BaseRequestHandler {

    private final LockRepository lockRepository;

    @Inject
    public Login(UserRepository userRepository,
                 WorkspaceRepository workspaceRepository,
                 Configuration configuration,
                 LockRepository lockRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.lockRepository = lockRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String username = UrlUtils.urlDecode(request.getParameter("username")).trim().toLowerCase();

        User user = lockRepository.lock("USER_" + username, new Callable<User>() {
                    @Override
                    public User call() throws Exception {
                        User user = getUserRepository().findByUsername(username);
                        if (user == null) {
                            // For form based authentication, username and displayName will be the same
                            String randomPassword = UserRepository.createRandomPassword();
                            user = getUserRepository().addUser(username, username, null, randomPassword, new String[0]);
                        }
                        return user;
                    }
                }
        );

        getUserRepository().recordLogin(user, AuthenticationHandler.getRemoteAddr(request));

        CurrentUser.set(request, user.getUserId(), user.getUsername());
        JSONObject json = new JSONObject();
        json.put("status", "OK");
        respondWithJson(response, json);
    }
}
