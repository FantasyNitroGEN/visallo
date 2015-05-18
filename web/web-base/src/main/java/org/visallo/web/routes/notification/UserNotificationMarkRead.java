package org.visallo.web.routes.notification;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.notification.UserNotificationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import com.v5analytics.webster.HandlerChain;
import org.visallo.web.BaseRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserNotificationMarkRead extends BaseRequestHandler {
    private final UserNotificationRepository userNotificationRepository;
    private static final String IDS_PARAMETER_NAME = "notificationIds[]";

    @Inject
    public UserNotificationMarkRead(
            final UserNotificationRepository userNotificationRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.userNotificationRepository = userNotificationRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String[] notificationIds = getRequiredParameterArray(request, IDS_PARAMETER_NAME);
        userNotificationRepository.markRead(notificationIds, getUser(request));
        respondWithSuccessJson(response);
    }
}
