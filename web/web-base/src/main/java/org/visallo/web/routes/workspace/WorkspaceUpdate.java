package org.visallo.web.routes.workspace;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.notification.ExpirationAge;
import org.visallo.core.model.notification.ExpirationAgeUnit;
import org.visallo.core.model.notification.UserNotificationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiWorkspace;
import org.visallo.web.clientapi.model.ClientApiWorkspaceUpdateData;
import org.visallo.web.clientapi.model.GraphPosition;
import org.visallo.web.clientapi.model.WorkspaceAccess;
import org.visallo.web.clientapi.util.ObjectMapperFactory;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

public class WorkspaceUpdate extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspaceUpdate.class);
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private final UserNotificationRepository userNotificationRepository;

    @Inject
    public WorkspaceUpdate(
            final WorkspaceRepository workspaceRepository,
            final UserRepository userRepository,
            final WorkQueueRepository workQueueRepository,
            final UserNotificationRepository userNotificationRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
        this.userNotificationRepository = userNotificationRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String workspaceId = getActiveWorkspaceId(request);
        final String data = getRequiredParameter(request, "data");

        User authUser = getUser(request);
        Authorizations authorizations = getAuthorizations(request, authUser);

        Workspace workspace = workspaceRepository.findById(workspaceId, authUser);
        if (workspace == null) {
            respondWithNotFound(response);
            return;
        }

        ClientApiWorkspaceUpdateData updateData = ObjectMapperFactory.getInstance().readValue(data, ClientApiWorkspaceUpdateData.class);

        if (updateData.getTitle() != null) {
            setTitle(workspace, updateData.getTitle(), authUser);
        }

        updateEntities(workspace, updateData.getEntityUpdates(), authUser);

        deleteEntities(workspace, updateData.getEntityDeletes(), authUser);

        ResourceBundle resource = getBundle(request);
        String title = resource.getString("workspaces.notification.shared.title");
        String message = resource.getString("workspaces.notification.shared.subtitle");
        updateUsers(workspace, updateData.getUserUpdates(), authUser, title, message);

        workspace = workspaceRepository.findById(workspaceId, authUser);
        ClientApiWorkspace clientApiWorkspaceAfterUpdateButBeforeDelete = workspaceRepository.toClientApi(workspace, authUser, true, authorizations);
        List<ClientApiWorkspace.User> previousUsers = clientApiWorkspaceAfterUpdateButBeforeDelete.getUsers();
        deleteUsers(workspace, updateData.getUserDeletes(), authUser);

        ClientApiWorkspace clientApiWorkspace = workspaceRepository.toClientApi(workspace, authUser, true, authorizations);

        respondWithSuccessJson(response);

        workQueueRepository.pushWorkspaceChange(clientApiWorkspace, previousUsers, authUser.getUserId(), request.getSession().getId());
    }

    private void setTitle(Workspace workspace, String title, User authUser) {
        LOGGER.debug("setting title (%s): %s", workspace.getWorkspaceId(), title);
        workspaceRepository.setTitle(workspace, title, authUser);
    }

    private void deleteUsers(Workspace workspace, List<String> userDeletes, User authUser) {
        for (String userId : userDeletes) {
            LOGGER.debug("user delete (%s): %s", workspace.getWorkspaceId(), userId);
            workspaceRepository.deleteUserFromWorkspace(workspace, userId, authUser);
            workQueueRepository.pushWorkspaceDelete(workspace.getWorkspaceId(), userId);
        }
    }

    private void updateUsers(Workspace workspace, List<ClientApiWorkspaceUpdateData.UserUpdate> userUpdates, User authUser, String title, String subtitle) {
        for (ClientApiWorkspaceUpdateData.UserUpdate update : userUpdates) {
            LOGGER.debug("user update (%s): %s", workspace.getWorkspaceId(), update.toString());
            String userId = update.getUserId();
            WorkspaceAccess workspaceAccess = update.getAccess();
            workspaceRepository.updateUserOnWorkspace(workspace, userId, workspaceAccess, authUser);

            String message = MessageFormat.format(subtitle, authUser.getDisplayName(), workspace.getDisplayTitle());
            JSONObject payload = new JSONObject();
            payload.put("workspaceId", workspace.getWorkspaceId());
            userNotificationRepository.createNotification(userId, title, message, "switchWorkspace", payload, new ExpirationAge(7, ExpirationAgeUnit.DAY), authUser);
        }
    }

    private void deleteEntities(Workspace workspace, List<String> entityIdsToDelete, User authUser) {
        workspaceRepository.softDeleteEntitiesFromWorkspace(workspace, entityIdsToDelete, authUser);
    }

    private void updateEntities(Workspace workspace, List<ClientApiWorkspaceUpdateData.EntityUpdate> entityUpdates, User authUser) {
        List<WorkspaceRepository.Update> updates = Lists.transform(entityUpdates, new Function<ClientApiWorkspaceUpdateData.EntityUpdate, WorkspaceRepository.Update>() {
            @Nullable
            @Override
            public WorkspaceRepository.Update apply(ClientApiWorkspaceUpdateData.EntityUpdate u) {
                String vertexId = u.getVertexId();
                GraphPosition graphPosition = u.getGraphPosition();
                String graphLayoutJson = u.getGraphLayoutJson();
                return new WorkspaceRepository.Update(vertexId, true, graphPosition, graphLayoutJson);
            }
        });
        workspaceRepository.updateEntitiesOnWorkspace(workspace, updates, authUser);
    }
}
