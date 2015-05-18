package org.visallo.core.model.workspace;

import org.visallo.web.clientapi.model.WorkspaceAccess;

public class WorkspaceUser {
    private final String userId;
    private final WorkspaceAccess workspaceAccess;
    private boolean isCreator = false;

    public WorkspaceUser(String userId, WorkspaceAccess workspaceAccess, boolean isCreator) {
        this.userId = userId;
        this.workspaceAccess = workspaceAccess;
        this.isCreator = isCreator;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isCreator() {
        return isCreator;
    }

    public WorkspaceAccess getWorkspaceAccess() {
        return workspaceAccess;
    }
}
