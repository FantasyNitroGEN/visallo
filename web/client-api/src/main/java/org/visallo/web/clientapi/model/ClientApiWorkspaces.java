package org.visallo.web.clientapi.model;

import org.visallo.web.clientapi.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ClientApiWorkspaces implements ClientApiObject {
    private List<ClientApiWorkspace> workspaces = new ArrayList<ClientApiWorkspace>();

    public List<ClientApiWorkspace> getWorkspaces() {
        return workspaces;
    }

    @Override
    public String toString() {
        return "Workspaces{" +
                "workspaces=" + StringUtils.join(workspaces) +
                '}';
    }

    public void addWorkspace(ClientApiWorkspace workspace) {
        this.workspaces.add(workspace);
    }
}

