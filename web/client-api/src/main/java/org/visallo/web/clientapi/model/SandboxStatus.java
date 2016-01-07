package org.visallo.web.clientapi.model;

import java.util.Set;

public enum SandboxStatus {
    PUBLIC,
    PUBLIC_CHANGED,
    PRIVATE;

    public static SandboxStatus getFromVisibilityString(String visibility, String workspaceId) {
        if (visibility == null) {
            return SandboxStatus.PUBLIC;
        }
        if (workspaceId == null || !visibility.contains(workspaceId)) {
            return SandboxStatus.PUBLIC;
        }
        return SandboxStatus.PRIVATE;
    }

    public static SandboxStatus getFromVisibilityJsonString(VisibilityJson visibilityJson, String workspaceId) {
        if (visibilityJson == null) {
            return SandboxStatus.PUBLIC;
        }
        Set<String> workspacesList = visibilityJson.getWorkspaces();
        if (workspacesList == null || workspacesList.size() == 0) {
            return SandboxStatus.PUBLIC;
        }
        if (workspaceId == null || !workspacesList.contains(workspaceId)) {
            return SandboxStatus.PUBLIC;
        }
        return SandboxStatus.PRIVATE;
    }
}
