package org.visallo.web.routes.workspace;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.Authorizations;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiWorkspace;
import org.visallo.web.clientapi.model.ClientApiWorkspaces;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorkspaceList extends BaseRequestHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceList(
            final WorkspaceRepository workspaceRepository,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        String workspaceId = getWorkspaceIdOrDefault(request);
        Authorizations authorizations = null;
        if (workspaceId != null && workspaceRepository.hasReadPermissions(workspaceId, user)) {
            authorizations = userRepository.getAuthorizations(user, workspaceId);
        } else {
            authorizations = userRepository.getAuthorizations(user);
        }
        ClientApiWorkspaces results = handle(user, authorizations);
        respondWithClientApiObject(response, results);
    }

    public ClientApiWorkspaces handle(User user, Authorizations authorizations) {
        Iterable<Workspace> workspaces = workspaceRepository.findAllForUser(user);
        String activeWorkspaceId = getUserRepository().getCurrentWorkspaceId(user.getUserId());
        activeWorkspaceId = activeWorkspaceId != null ? activeWorkspaceId : "";

        ClientApiWorkspaces results = new ClientApiWorkspaces();
        for (Workspace workspace : workspaces) {
            ClientApiWorkspace workspaceClientApi = workspaceRepository.toClientApi(workspace, user, false, authorizations);
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
