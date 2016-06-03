package org.visallo.web.auth.usernameonly.routes;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.utils.UrlUtils;
import org.json.JSONObject;
import org.visallo.core.model.user.AuthorizationContext;
import org.visallo.core.model.user.AuthorizationMapper;
import org.visallo.core.model.user.UserNameAuthorizationContext;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.AuthenticationHandler;
import org.visallo.web.CurrentUser;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

public class Login implements ParameterizedHandler {
    private final UserRepository userRepository;
    private final AuthorizationMapper authorizationMapper;

    @Inject
    public Login(UserRepository userRepository, AuthorizationMapper authorizationMapper) {
        this.userRepository = userRepository;
        this.authorizationMapper = authorizationMapper;
    }

    @Handle
    public JSONObject handle(
            HttpServletRequest request
    ) throws Exception {
        final String username = UrlUtils.urlDecode(request.getParameter("username")).trim().toLowerCase();
        User user = userRepository.findByUsername(username);

        AuthorizationContext authorizationContext = new UserNameAuthorizationContext(user, username);
        Set<String> privileges = authorizationMapper.getPrivileges(authorizationContext);
        Set<String> authorizations = authorizationMapper.getAuthorizations(authorizationContext);
        if (user == null) {
            // For form based authentication, username and displayName will be the same
            String randomPassword = UserRepository.createRandomPassword();
            user = userRepository.findOrAddUser(
                    username,
                    username,
                    null,
                    randomPassword,
                    privileges,
                    authorizations
            );
        } else {
            userRepository.setPrivileges(user, privileges, userRepository.getSystemUser());
            userRepository.setAuthorizations(user, authorizations, userRepository.getSystemUser());
        }

        userRepository.recordLogin(user, AuthenticationHandler.getRemoteAddr(request));

        CurrentUser.set(request, user.getUserId(), user.getUsername());
        JSONObject json = new JSONObject();
        json.put("status", "OK");
        return json;
    }
}
