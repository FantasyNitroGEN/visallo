package org.visallo.web.routes.dashboard;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiDashboardItemUpdateResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import static com.google.common.base.Preconditions.checkNotNull;

public class DashboardItemUpdate implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public DashboardItemUpdate(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiDashboardItemUpdateResponse handle(
            @Optional(name = "dashboardId") String dashboardId,
            @Optional(name = "dashboardItemId") String dashboardItemId,
            @Optional(name = "title") String title,
            @Optional(name = "configuration") String configuration,
            @Optional(name = "extensionId") String extensionId,
            @ActiveWorkspaceId String workspaceId,
            User user
    ) throws Exception {
        if (dashboardItemId == null) {
            checkNotNull(dashboardId, "dashboardId is required");
        }
        dashboardItemId = workspaceRepository.addOrUpdateDashboardItem(workspaceId, dashboardId, dashboardItemId, title, configuration, extensionId, user);
        return new ClientApiDashboardItemUpdateResponse(dashboardItemId);
    }
}
