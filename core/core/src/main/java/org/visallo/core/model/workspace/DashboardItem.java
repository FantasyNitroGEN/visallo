package org.visallo.core.model.workspace;

import java.io.Serializable;

public abstract class DashboardItem implements Serializable {
    static long serialVersionUID = 1L;
    private final String id;
    private final String extensionId;

    public DashboardItem(String id, String extensionId) {
        this.id = id;
        this.extensionId = extensionId;
    }

    public String getId() {
        return id;
    }

    public String getExtensionId() {
        return extensionId;
    }

    public abstract String getTitle();

    public abstract String getConfiguration();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DashboardItem that = (DashboardItem) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "DashboardItem{" +
                "title='" + getTitle() + '\'' +
                ", id='" + id + '\'' +
                ", extensionId='" + extensionId + '\'' +
                '}';
    }
}
