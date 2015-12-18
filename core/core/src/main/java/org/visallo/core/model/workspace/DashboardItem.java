package org.visallo.core.model.workspace;

public class DashboardItem {
    private final String id;
    private final String extensionId;
    private final String title;
    private final String configuration;

    public DashboardItem(String id, String extensionId, String title, String configuration) {
        this.id = id;
        this.extensionId = extensionId;
        this.title = title;
        this.configuration = configuration;
    }

    public String getId() {
        return id;
    }

    public String getExtensionId() {
        return extensionId;
    }

    public String getTitle() {
        return title;
    }

    public String getConfiguration() {
        return configuration;
    }
}
