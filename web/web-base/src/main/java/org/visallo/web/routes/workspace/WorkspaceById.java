package org.visallo.web.routes.workspace;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.SecurityVertexiumException;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiWorkspace;

public class WorkspaceById implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspaceById.class);
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceById(
            final WorkspaceRepository workspaceRepository
    ) {
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiWorkspace handle(
            @Required(name = "workspaceId") String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        LOGGER.info("Attempting to retrieve workspace: %s", workspaceId);
        try {
            final Workspace workspace = workspaceRepository.findById(workspaceId, user);
            if (workspace == null) {
                throw new VisalloResourceNotFoundException("Could not find workspace: " + workspaceId);
            } else {
                LOGGER.debug("Successfully found workspace");
                return workspaceRepository.toClientApi(workspace, user, true, authorizations);
            }
        } catch (SecurityVertexiumException ex) {
            throw new VisalloAccessDeniedException("Could not get workspace " + workspaceId, user, workspaceId);
        }
    }
}
