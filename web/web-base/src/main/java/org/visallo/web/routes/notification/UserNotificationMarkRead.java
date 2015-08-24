package org.visallo.web.routes.notification;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.model.notification.UserNotificationRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;

public class UserNotificationMarkRead implements ParameterizedHandler {
    private final UserNotificationRepository userNotificationRepository;

    @Inject
    public UserNotificationMarkRead(final UserNotificationRepository userNotificationRepository) {
        this.userNotificationRepository = userNotificationRepository;
    }

    @Handle
    public void handle(
            User user,
            @Required(name = "notificationIds[]") String[] notificationIds,
            VisalloResponse response
    ) throws Exception {
        userNotificationRepository.markRead(notificationIds, user);
        response.respondWithSuccessJson();
    }
}
