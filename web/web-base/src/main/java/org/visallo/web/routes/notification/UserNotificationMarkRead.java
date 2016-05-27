package org.visallo.web.routes.notification;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.notification.UserNotification;
import org.visallo.core.model.notification.UserNotificationRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;

public class UserNotificationMarkRead implements ParameterizedHandler {
    private final UserNotificationRepository userNotificationRepository;

    @Inject
    public UserNotificationMarkRead(final UserNotificationRepository userNotificationRepository) {
        this.userNotificationRepository = userNotificationRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "notificationIds[]") String[] notificationIds,
            User user
    ) throws Exception {
        for (String notificationId : notificationIds) {
            UserNotification notification = userNotificationRepository.getNotification(notificationId, user);
            if (notification == null) {
                throw new VisalloResourceNotFoundException("Could not find notification with id: " + notificationId);
            }
            if (!notification.getUserId().equals(user.getUserId())) {
                throw new VisalloAccessDeniedException(
                        "Cannot mark notification read that do not belong to you",
                        user,
                        notificationId
                );
            }
        }

        userNotificationRepository.markRead(notificationIds, user);
        return VisalloResponse.SUCCESS;
    }
}
