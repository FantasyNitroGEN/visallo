package org.visallo.core.model.workspace;

import java.util.Collection;
import java.util.List;

public class Dashboard {
    private final String id;
    private final String workspaceId;
    private final String title;
    private final List<DashboardItem> items;

    public Dashboard(String id, String workspaceId, String title, List<DashboardItem> items) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.title = title;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getTitle() {
        return title;
    }

    public Collection<DashboardItem> getItems() {
        return items;
    }
}
