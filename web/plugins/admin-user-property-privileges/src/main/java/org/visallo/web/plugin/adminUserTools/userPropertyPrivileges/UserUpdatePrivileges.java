package org.visallo.web.plugin.adminUserTools.userPropertyPrivileges;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.user.UserPropertyPrivilegeRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.Privilege;

import java.util.Set;

public class UserUpdatePrivileges implements ParameterizedHandler {
    private final UserRepository userRepository;
    private final UserPropertyPrivilegeRepository privilegeRepository;

    @Inject
    public UserUpdatePrivileges(UserRepository userRepository, PrivilegeRepository privilegeRepository) {
        if (!(privilegeRepository instanceof UserPropertyPrivilegeRepository)) {
            throw new VisalloException(UserPropertyPrivilegeRepository.class.getName() + " required");
        }

        this.userRepository = userRepository;
        this.privilegeRepository = (UserPropertyPrivilegeRepository) privilegeRepository;
    }

    @Handle
    public JSONObject handle(
            @Required(name = "user-name") String userName,
            @Required(name = "privileges") String privilegesParameter,
            User authUser
    ) throws Exception {
        Set<String> privileges = Privilege.stringToPrivileges(privilegesParameter);

        User user = userRepository.findByUsername(userName);
        if (user == null) {
            throw new VisalloResourceNotFoundException("Could not find user: " + userName);
        }

        privilegeRepository.setPrivileges(user, privileges, authUser);

        return userRepository.toJsonWithAuths(user);
    }
}
