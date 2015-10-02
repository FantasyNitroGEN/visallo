package org.visallo.web.routes.notification;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.model.notification.SystemNotification;
import org.visallo.core.model.notification.SystemNotificationRepository;
import org.visallo.core.model.notification.SystemNotificationSeverity;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SystemNotificationSave implements ParameterizedHandler {
    private final SystemNotificationRepository systemNotificationRepository;
    private final WorkQueueRepository workQueueRepository;
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm 'UTC'";

    @Inject
    public SystemNotificationSave(
            final SystemNotificationRepository systemNotificationRepository,
            final WorkQueueRepository workQueueRepository
    ) {
        this.systemNotificationRepository = systemNotificationRepository;
        this.workQueueRepository = workQueueRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Optional(name = "notificationId") String notificationId,
            @Required(name = "severity") SystemNotificationSeverity severity,
            @Required(name = "title") String title,
            @Required(name = "message") String message,
            @Required(name = "startDate") String startDateParameter,
            @Optional(name = "endDate") String endDateParameter,
            @Optional(name = "externalUrl") String externalUrl,
            User user
    ) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date startDate = sdf.parse(startDateParameter);
        Date endDate = endDateParameter != null ? sdf.parse(endDateParameter) : null;

        SystemNotification notification;

        if (notificationId == null) {
            notification = systemNotificationRepository.createNotification(severity, title, message, externalUrl, startDate, endDate, user);
        } else {
            notification = systemNotificationRepository.getNotification(notificationId, user);
            notification.setSeverity(severity);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setStartDate(startDate);
            notification.setEndDate(endDate);
            if (externalUrl != null) {
                notification.setExternalUrl(externalUrl);
            }
            notification = systemNotificationRepository.updateNotification(notification, user);
        }

        if (notification.isActive()) {
            workQueueRepository.pushSystemNotification(notification);
        } else {
            workQueueRepository.pushSystemNotificationUpdate(notification);
        }

        return VisalloResponse.SUCCESS;
    }
}
