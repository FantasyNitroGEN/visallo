package org.visallo.web.devTools.user;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;

public class UserAddAuthorization implements ParameterizedHandler {
    private final UserRepository userRepository;

    @Inject
    public UserAddAuthorization(final UserRepository userRepository) {
        this.userRepository = userRepository;
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
        userRepository.addAuthorization(user, auth, authUser);
        return userRepository.toJsonWithAuths(user);
    }
}
