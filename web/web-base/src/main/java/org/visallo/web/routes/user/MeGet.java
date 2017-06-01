package org.visallo.web.routes.user;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.handlers.CSRFHandler;
import org.vertexium.util.IterableUtils;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiUser;

import javax.servlet.http.HttpServletRequest;

public class MeGet implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(MeGet.class);
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public MeGet(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository
    ) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiUser handle(
            HttpServletRequest request,
            User user
    ) throws Exception {
        ClientApiUser userMe = userRepository.toClientApiPrivate(user);
        userMe.setCsrfToken(CSRFHandler.getSavedToken(request, true));

        try {
            if (userMe.getCurrentWorkspaceId() != null && userMe.getCurrentWorkspaceId().length() > 0) {
                if (!workspaceRepository.hasReadPermissions(userMe.getCurrentWorkspaceId(), user)) {
                    userMe.setCurrentWorkspaceId(null);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to read user's current workspace %s", user.getCurrentWorkspaceId(), ex);
            userMe.setCurrentWorkspaceId(null);
        }

        if (userMe.getCurrentWorkspaceId() == null) {
            Workspace workspace = workspaceRepository.add(user);
            userMe.setCurrentWorkspaceId(workspace.getWorkspaceId());
            userMe.setCurrentWorkspaceName(workspace.getDisplayTitle());
        }

        return userMe;
    }
}
