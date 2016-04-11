package org.visallo.core.model.workspace;

import java.io.Serializable;

import static org.visallo.core.util.StreamUtil.stream;

public abstract class Dashboard implements Serializable {
    static long serialVersionUID = 1L;
    private final String id;
    private final String workspaceId;

    public Dashboard(String id, String workspaceId) {
        this.id = id;
        this.workspaceId = workspaceId;
    }

    public String getId() {
        return id;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public abstract String getTitle();

    public abstract Iterable<? extends DashboardItem> getItems();

    public DashboardItem getItemById(String dashboardItemId) {
        return stream(getItems())
                .filter(dashboardItem -> dashboardItem.getId().equals(dashboardItemId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Dashboard dashboard = (Dashboard) o;

        return id.equals(dashboard.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Dashboard{" +
                "title='" + getTitle() + '\'' +
                ", id='" + id + '\'' +
                ", workspaceId='" + workspaceId + '\'' +
                '}';
    }
}
