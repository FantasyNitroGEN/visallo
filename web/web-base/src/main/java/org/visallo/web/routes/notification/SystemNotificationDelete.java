package org.visallo.web.routes.notification;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.notification.SystemNotification;
import org.visallo.core.model.notification.SystemNotificationRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;

public class SystemNotificationDelete implements ParameterizedHandler {
    private final SystemNotificationRepository systemNotificationRepository;
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public SystemNotificationDelete(
            final SystemNotificationRepository systemNotificationRepository,
            final WorkQueueRepository workQueueRepository
    ) {
        this.systemNotificationRepository = systemNotificationRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = "notificationId") String notificationId,
            User user
    ) throws Exception {
        SystemNotification notification = systemNotificationRepository.getNotification(notificationId, user);
        if (notification == null) {
            throw new VisalloResourceNotFoundException("Could not find notification with id: " + notificationId);
        }

        systemNotificationRepository.endNotification(notification, user);
        workQueueRepository.pushSystemNotificationEnded(notificationId);
        return VisalloResponse.SUCCESS;
    }
}
