package org.visallo.web.clientapi.model;

public class ClientApiDashboardItemUpdateResponse implements ClientApiObject {
    private final String dashboardItemId;

    public ClientApiDashboardItemUpdateResponse(String dashboardItemId) {
        this.dashboardItemId = dashboardItemId;
    }

    public String getDashboardItemId() {
        return dashboardItemId;
    }
}
