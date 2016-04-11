package org.visallo.core.model.workspace;

import org.visallo.web.clientapi.model.WorkspaceAccess;

import java.io.Serializable;

public class WorkspaceUser implements Serializable {
    static long serialVersionUID = 1L;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WorkspaceUser that = (WorkspaceUser) o;

        return userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}
