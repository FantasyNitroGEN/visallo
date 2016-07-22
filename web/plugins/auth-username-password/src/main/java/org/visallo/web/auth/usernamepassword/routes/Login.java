package org.visallo.web.auth.usernamepassword.routes;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.user.UserNameAuthorizationContext;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.AuthenticationHandler;
import org.visallo.web.CurrentUser;

import javax.servlet.http.HttpServletRequest;

public class Login implements ParameterizedHandler {
    private final UserRepository userRepository;

    @Inject
    public Login(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Handle
    public JSONObject handle(
            HttpServletRequest request,
            @Required(name = "username") String username,
            @Required(name = "password") String password
    ) throws Exception {
        username = username.trim();
        password = password.trim();

        User user = userRepository.findByUsername(username);
        if (user != null && userRepository.isPasswordValid(user, password)) {
            UserNameAuthorizationContext authorizationContext = new UserNameAuthorizationContext(
                    username,
                    AuthenticationHandler.getRemoteAddr(request)
            );
            userRepository.updateUser(user, authorizationContext);
            CurrentUser.set(request, user.getUserId(), user.getUsername());
            JSONObject json = new JSONObject();
            json.put("status", "OK");
            return json;
        } else {
            throw new VisalloAccessDeniedException("", user, null);
        }
    }
}
