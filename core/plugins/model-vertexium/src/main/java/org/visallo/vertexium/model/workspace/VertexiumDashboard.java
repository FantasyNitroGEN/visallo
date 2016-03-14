package org.visallo.vertexium.model.workspace;

import org.visallo.core.model.workspace.Dashboard;
import org.visallo.core.model.workspace.DashboardItem;

import java.util.List;

public class VertexiumDashboard extends Dashboard {
    private final String title;
    private final List<DashboardItem> items;

    public VertexiumDashboard(String id, String workspaceId, String title, List<DashboardItem> items) {
        super(id, workspaceId);
        this.title = title;
        this.items = items;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public Iterable<DashboardItem> getItems() {
        return this.items;
    }
}
