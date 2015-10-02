package org.visallo.web.routes.notification;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import org.apache.commons.lang.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.visallo.core.model.notification.SystemNotification;
import org.visallo.core.model.notification.SystemNotificationRepository;
import org.visallo.core.model.notification.UserNotification;
import org.visallo.core.model.notification.UserNotificationRepository;
import org.visallo.core.user.User;

import java.util.Date;

public class Notifications implements ParameterizedHandler {
    private final SystemNotificationRepository systemNotificationRepository;
    private final UserNotificationRepository userNotificationRepository;

    @Inject
    public Notifications(
            final SystemNotificationRepository systemNotificationRepository,
            final UserNotificationRepository userNotificationRepository
    ) {
        this.systemNotificationRepository = systemNotificationRepository;
        this.userNotificationRepository = userNotificationRepository;
    }

    @Handle
    public JSONObject handle(
            @Optional(name = "futureDays", defaultValue = "10") int futureDays,
            User user
    ) throws Exception {
        JSONObject notifications = new JSONObject();

        JSONObject systemNotifications = new JSONObject();

        JSONArray activeNotifications = new JSONArray();
        for (SystemNotification notification : systemNotificationRepository.getActiveNotifications(user)) {
            activeNotifications.put(notification.toJSONObject());
        }
        systemNotifications.put("active", activeNotifications);

        Date maxDate = DateUtils.addDays(new Date(), futureDays);
        JSONArray futureNotifications = new JSONArray();
        for (SystemNotification notification : systemNotificationRepository.getFutureNotifications(maxDate, user)) {
            futureNotifications.put(notification.toJSONObject());
        }
        systemNotifications.put("future", futureNotifications);

        JSONArray userNotifications = new JSONArray();
        for (UserNotification notification : userNotificationRepository.getActiveNotifications(user)) {
            userNotifications.put(notification.toJSONObject());
        }

        notifications.put("system", systemNotifications);
        notifications.put("user", userNotifications);
        return notifications;
    }
}
