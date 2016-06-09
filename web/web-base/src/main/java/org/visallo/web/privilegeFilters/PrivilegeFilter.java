package org.visallo.web.privilegeFilters;

import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.RequestResponseHandler;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.parameterProviders.VisalloBaseParameterProvider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

public class PrivilegeFilter implements RequestResponseHandler {
    private final Set<String> requiredPrivileges;
    private UserRepository userRepository;
    private final PrivilegeRepository privilegeRepository;

    protected PrivilegeFilter(
            Set<String> requiredPrivileges,
            UserRepository userRepository,
            PrivilegeRepository privilegeRepository
    ) {
        this.requiredPrivileges = requiredPrivileges;
        this.userRepository = userRepository;
        this.privilegeRepository = privilegeRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = VisalloBaseParameterProvider.getUser(request, userRepository);
        if (!privilegeRepository.hasAllPrivileges(user, requiredPrivileges)) {
            throw new VisalloAccessDeniedException(
                    "You do not have the required privileges: " + Privilege.toString(requiredPrivileges),
                    user,
                    "privileges"
            );
        }
        chain.next(request, response);
    }
}
