package org.visallo.web.routes.notification;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.model.notification.SystemNotification;
import org.visallo.core.model.notification.SystemNotificationRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;

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
    public void handle(
            User user,
            @Required(name = "notificationId") String notificationId,
            VisalloResponse response
    ) throws Exception {
        SystemNotification notification = systemNotificationRepository.getNotification(notificationId, user);
        if (notification == null) {
            response.respondWithNotFound();
            return;
        }

        systemNotificationRepository.endNotification(notification, user);
        workQueueRepository.pushSystemNotificationEnded(notificationId);
        response.respondWithSuccessJson();
    }
}
