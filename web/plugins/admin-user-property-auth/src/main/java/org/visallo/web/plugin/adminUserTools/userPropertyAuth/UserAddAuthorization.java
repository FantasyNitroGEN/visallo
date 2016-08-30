package org.visallo.web.plugin.adminUserTools.userPropertyAuth;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UpdatableAuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;

public class UserAddAuthorization implements ParameterizedHandler {
    private final AuthorizationRepository authorizationRepository;
    private final UserRepository userRepository;

    @Inject
    public UserAddAuthorization(
            UserRepository userRepository,
            AuthorizationRepository authorizationRepository
    ) {
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
    }

    @Handle
    public JSONObject handle(
            @Required(name = "user-name") String userName,
            @Required(name = "auth") String auth,
            User authUser
    ) throws Exception {
        User user = userRepository.findByUsername(userName);
        if (user == null) {
            throw new VisalloResourceNotFoundException("User " + userName + " not found");
        }

        if (!(authorizationRepository instanceof UpdatableAuthorizationRepository)) {
            throw new VisalloAccessDeniedException("Authorization repository does not support updating", authUser, userName);
        }

        ((UpdatableAuthorizationRepository) authorizationRepository).addAuthorization(user, auth, authUser);
        return userRepository.toJsonWithAuths(user);
    }
}
