package org.visallo.web.routes.user;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.handlers.CSRFHandler;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MeGet extends BaseRequestHandler {
    @Inject
    public MeGet(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        if (user == null || user.getUsername() == null) {
            respondWithNotFound(response);
            return;
        }

        ClientApiUser userMe = getUserRepository().toClientApiPrivate(user);
        userMe.setCsrfToken(CSRFHandler.getSavedToken(request, true));

        respondWithClientApiObject(response, userMe);
    }
}
