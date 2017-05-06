package org.visallo.web.routes.workspace;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.formula.FormulaEvaluator;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiWorkspaceDiff;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

public class WorkspaceDiff implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public WorkspaceDiff(final WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiWorkspaceDiff handle(
            @ActiveWorkspaceId String workspaceId,
            FormulaEvaluator.UserContext userContext,
            User user
    ) throws Exception {
        Workspace workspace = workspaceRepository.findById(workspaceId, true, user);
        if (workspace == null) {
            throw new VisalloResourceNotFoundException("Cannot find workspace: " + workspaceId);
        }

        return this.workspaceRepository.getDiff(workspace, user, userContext);
    }
}
