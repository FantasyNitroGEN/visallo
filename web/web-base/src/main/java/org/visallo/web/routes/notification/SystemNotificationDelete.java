package org.visallo.web.routes.notification;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.notification.SystemNotification;
import org.visallo.core.model.notification.SystemNotificationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.BaseRequestHandler;
import com.v5analytics.webster.HandlerChain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SystemNotificationDelete extends BaseRequestHandler {
    private final SystemNotificationRepository systemNotificationRepository;
    private final WorkQueueRepository workQueueRepository;
    private static final String ID_PARAMETER_NAME = "notificationId";

    @Inject
    public SystemNotificationDelete(
            final SystemNotificationRepository systemNotificationRepository,
            final WorkQueueRepository workQueueRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.systemNotificationRepository = systemNotificationRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String notificationId = getRequiredParameter(request, ID_PARAMETER_NAME);
        User user = getUser(request);

        SystemNotification notification = systemNotificationRepository.getNotification(notificationId, getUser(request));
        if (notification == null) {
            respondWithNotFound(response);
            return;
        }

        systemNotificationRepository.endNotification(notification, user);
        workQueueRepository.pushSystemNotificationEnded(notificationId);

        respondWithSuccessJson(response);
    }
}
