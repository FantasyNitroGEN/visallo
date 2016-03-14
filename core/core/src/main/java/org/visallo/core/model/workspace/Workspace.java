package org.visallo.core.model.workspace;

import java.io.Serializable;

public interface Workspace extends Serializable {
    long serialVersionUID = 1L;

    String getWorkspaceId();

    String getDisplayTitle();
}

