package org.visallo.web.devTools.user;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.Privilege;

import java.util.Set;

public class UserUpdatePrivileges implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(UserUpdatePrivileges.class);
    private final UserRepository userRepository;

    @Inject
    public UserUpdatePrivileges(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Handle
    public JSONObject handle(
            @Required(name = "user-name") String userName,
            @Required(name = "privileges") String privilegesParameter,
            User authUser
    ) throws Exception {
        Set<Privilege> privileges = Privilege.stringToPrivileges(privilegesParameter);

        User user = userRepository.findByUsername(userName);
        if (user == null) {
            throw new VisalloResourceNotFoundException("Could not find user: " + userName);
        }

        LOGGER.info("Setting user %s privileges to %s", user.getUserId(), Privilege.toString(privileges));
        userRepository.setPrivileges(user, privileges, authUser);

        return userRepository.toJsonWithAuths(user);
    }
}
