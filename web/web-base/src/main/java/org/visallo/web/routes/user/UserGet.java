package org.visallo.web.routes.user;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.Authorizations;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserGet extends BaseRequestHandler {
    @Inject
    public UserGet(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String userName = getRequiredParameter(request, "user-name");

        User user = this.getUserRepository().findByUsername(userName);
        if (user == null) {
            respondWithNotFound(response);
            return;
        }
        Authorizations authorizations = getUserRepository().getAuthorizations(user);

        ClientApiUser clientApiUser = getUserRepository().toClientApiPrivate(user);

        Iterable<Workspace> workspaces = getWorkspaceRepository().findAllForUser(user);
        for (Workspace workspace : workspaces) {
            clientApiUser.getWorkspaces().add(getWorkspaceRepository().toClientApi(workspace, user, false, authorizations));
        }

        respondWithClientApiObject(response, clientApiUser);
    }
}
