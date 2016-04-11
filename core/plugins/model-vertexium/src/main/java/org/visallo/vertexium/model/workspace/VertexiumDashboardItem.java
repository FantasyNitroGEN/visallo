package org.visallo.vertexium.model.workspace;

import org.visallo.core.model.workspace.DashboardItem;

public class VertexiumDashboardItem extends DashboardItem {
    private final String title;
    private final String configuration;

    public VertexiumDashboardItem(String id, String extensionId, String title, String configuration) {
        super(id, extensionId);
        this.title = title;
        this.configuration = configuration;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getConfiguration() {
        return configuration;
    }
}
