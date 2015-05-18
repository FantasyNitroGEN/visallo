package org.visallo.web.routes.workspace;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiWorkspace;
import org.vertexium.SecurityVertexiumException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorkspaceById extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspaceById.class);

    @Inject
    public WorkspaceById(
            final WorkspaceRepository workspaceRepo,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String workspaceId = super.getAttributeString(request, "workspaceId");
        final User authUser = getUser(request);
        LOGGER.info("Attempting to retrieve workspace: %s", workspaceId);
        try {
            final Workspace workspace = getWorkspaceRepository().findById(workspaceId, authUser);
            if (workspace == null) {
                LOGGER.warn("Could not find workspace: %s", workspaceId);
                respondWithNotFound(response);
            } else {
                LOGGER.debug("Successfully found workspace");
                ClientApiWorkspace result = getWorkspaceRepository().toClientApi(workspace, authUser, true);
                respondWithClientApiObject(response, result);
            }
        } catch (SecurityVertexiumException ex) {
            LOGGER.error("security error", ex);
            respondWithAccessDenied(response, "Could not get workspace");
        }
    }
}
