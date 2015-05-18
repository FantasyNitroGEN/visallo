package org.visallo.web.routes.workspace;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import com.v5analytics.webster.HandlerChain;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiWorkspaceDiff;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

public class WorkspaceDiff extends BaseRequestHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceDiff(
            final WorkspaceRepository workspaceRepository,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        String workspaceId = getActiveWorkspaceId(request);
        Locale locale = getLocale(request);
        String timeZone = getTimeZone(request);
        ClientApiWorkspaceDiff diff = handle(workspaceId, user, locale, timeZone);
        respondWithClientApiObject(response, diff);
    }

    public ClientApiWorkspaceDiff handle(String workspaceId, User user, Locale locale, String timeZone) {
        Workspace workspace = workspaceRepository.findById(workspaceId, true, user);
        if (workspace == null) {
            return null;
        }

        return this.workspaceRepository.getDiff(workspace, user, locale, timeZone);
    }
}
