package org.visallo.web.routes.dashboard;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.workspace.Dashboard;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

public class DashboardDelete implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public DashboardDelete(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "dashboardId") String dashboardId,
            @ActiveWorkspaceId String workspaceId,
            User user
    ) throws Exception {

        Dashboard dashboard = workspaceRepository.findDashboardById(workspaceId, dashboardId, user);
        if (dashboard == null) {
            throw new VisalloResourceNotFoundException("Could not find dashboard with id " + dashboardId);
        }

        workspaceRepository.deleteDashboard(workspaceId, dashboardId, user);
        return VisalloResponse.SUCCESS;
    }
}
