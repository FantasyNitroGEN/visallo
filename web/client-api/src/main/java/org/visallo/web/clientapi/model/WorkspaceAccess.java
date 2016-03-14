package org.visallo.web.clientapi.model;

public enum WorkspaceAccess {
    NONE,
    READ,
    WRITE,
    COMMENT;

    public static boolean hasReadPermissions(WorkspaceAccess workspaceAccess) {
        return workspaceAccess == READ
                || workspaceAccess == WRITE
                || workspaceAccess == COMMENT;
    }

    public static boolean hasCommentPermissions(WorkspaceAccess workspaceAccess) {
        return workspaceAccess == WRITE
                || workspaceAccess == COMMENT;
    }

    public static boolean hasWritePermissions(WorkspaceAccess workspaceAccess) {
        return workspaceAccess == WRITE;
    }
}
