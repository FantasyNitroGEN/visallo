package org.visallo.web.routes.workspace;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import org.vertexium.Authorizations;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiWorkspace;

import java.util.ArrayList;

public class WorkspaceCreate implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspaceCreate.class);
    private static final String DEFAULT_WORKSPACE_TITLE = "Default";

    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public WorkspaceCreate(
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiWorkspace handle(
            @Optional(name = "title") String title,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Workspace workspace;
        if (title == null) {
            title = DEFAULT_WORKSPACE_TITLE + " - " + user.getDisplayName();
        }
        workspace = workspaceRepository.add(title, user);

        LOGGER.info("Created workspace: %s, title: %s", workspace.getWorkspaceId(), workspace.getDisplayTitle());
        ClientApiWorkspace clientApiWorkspace = workspaceRepository.toClientApi(workspace, user, true, authorizations);

        workQueueRepository.pushWorkspaceChange(clientApiWorkspace, new ArrayList<ClientApiWorkspace.User>(), user.getUserId(), null);

        return clientApiWorkspace;
    }
}
