package org.visallo.web.privilegeFilters;

import com.v5analytics.webster.HandlerChain;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.core.user.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

public class PrivilegeFilter extends BaseRequestHandler {
    private final Set<Privilege> requiredPrivileges;

    protected PrivilegeFilter(
            final Set<Privilege> requiredPrivileges,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.requiredPrivileges = requiredPrivileges;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Set<Privilege> userPrivileges = getPrivileges(user);
        if (!Privilege.hasAll(userPrivileges, requiredPrivileges)) {
            respondWithAccessDenied(response, "You do not have the required privileges: " + Privilege.toString(requiredPrivileges));
            return;
        }
        chain.next(request, response);
    }
}
