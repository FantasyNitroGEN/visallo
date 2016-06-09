package org.visallo.web.routes.workspace;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.vertexium.Authorizations;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiWorkspace;
import org.visallo.web.clientapi.model.ClientApiWorkspaces;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

public class WorkspaceList implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public WorkspaceList(
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            AuthorizationRepository authorizationRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
    }

    @Handle
    public ClientApiWorkspaces handle(
            @ActiveWorkspaceId(required = false) String workspaceId,
            User user
    ) throws Exception {
        Authorizations authorizations;
        if (workspaceId != null && workspaceRepository.hasReadPermissions(workspaceId, user)) {
            authorizations = authorizationRepository.getGraphAuthorizations(user, workspaceId);
        } else {
            authorizations = authorizationRepository.getGraphAuthorizations(user);
        }

        Iterable<Workspace> workspaces = workspaceRepository.findAllForUser(user);
        String activeWorkspaceId = userRepository.getCurrentWorkspaceId(user.getUserId());
        activeWorkspaceId = activeWorkspaceId != null ? activeWorkspaceId : "";

        ClientApiWorkspaces results = new ClientApiWorkspaces();
        for (Workspace workspace : workspaces) {
            ClientApiWorkspace workspaceClientApi = workspaceRepository.toClientApi(
                    workspace,
                    user,
                    false,
                    authorizations
            );
            if (workspaceClientApi != null) {
                if (activeWorkspaceId.equals(workspace.getWorkspaceId())) { //if its the active one
                    workspaceClientApi.setActive(true);
                }
                results.addWorkspace(workspaceClientApi);
            }
        }
        return results;
    }
}
