package org.visallo.web.routes.user;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiUser;

public class UserGet implements ParameterizedHandler {
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public UserGet(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository
    ) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiUser handle(
            @Required(name = "user-name") String userName
    ) throws Exception {
        User user = userRepository.findByUsername(userName);
        if (user == null) {
            throw new VisalloResourceNotFoundException("user not found");
        }

        Authorizations authorizations = userRepository.getAuthorizations(user);

        ClientApiUser clientApiUser = userRepository.toClientApiPrivate(user);

        Iterable<Workspace> workspaces = workspaceRepository.findAllForUser(user);
        for (Workspace workspace : workspaces) {
            clientApiUser.getWorkspaces().add(workspaceRepository.toClientApi(workspace, user, false, authorizations));
        }

        return clientApiUser;
    }
}
