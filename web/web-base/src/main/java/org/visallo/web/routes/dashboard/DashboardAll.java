package org.visallo.web.routes.dashboard;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.workspace.Dashboard;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiDashboards;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.Collection;

public class DashboardAll implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public DashboardAll(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Handle
    public ClientApiDashboards handle(
            @ActiveWorkspaceId String workspaceId,
            User user
    ) throws Exception {
        Collection<Dashboard> dashboards = workspaceRepository.findAllDashboardsForWorkspace(workspaceId, user);
        if (dashboards == null) {
            throw new VisalloResourceNotFoundException("Could not find dashboards for workspace " + workspaceId);
        }
        return ClientApiConverter.toClientApiDashboards(dashboards);
    }
}
