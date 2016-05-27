package org.visallo.core.model.notification;

import com.google.inject.Inject;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.json.JSONObject;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SystemNotificationRepository extends NotificationRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(SystemNotificationRepository.class);
    private static final String VISIBILITY_STRING = "";
    private final UserRepository userRepository;

    @Inject
    public SystemNotificationRepository(
            SimpleOrmSession simpleOrmSession,
            UserRepository userRepository
    ) {
        super(simpleOrmSession);
        this.userRepository = userRepository;
    }

    public List<SystemNotification> getActiveNotifications(User user) {
        Date now = new Date();
        List<SystemNotification> activeNotifications = new ArrayList<>();
        for (SystemNotification notification : getSimpleOrmSession().findAll(
                SystemNotification.class,
                userRepository.getSimpleOrmContext(user)
        )) {
            if (notification.getStartDate().before(now)) {
                if (notification.getEndDate() == null || notification.getEndDate().after(now)) {
                    activeNotifications.add(notification);
                }
            }
        }
        LOGGER.debug("returning %d active system notifications", activeNotifications.size());
        return activeNotifications;
    }

    public SystemNotification createNotification(
            SystemNotificationSeverity severity,
            String title,
            String message,
            String actionEvent,
            JSONObject actionPayload,
            Date startDate,
            Date endDate,
            User user
    ) {
        if (startDate == null) {
            startDate = new Date();
        }
        SystemNotification notification = new SystemNotification(startDate, title, message, actionEvent, actionPayload);
        notification.setSeverity(severity);
        notification.setStartDate(startDate);
        notification.setEndDate(endDate);
        getSimpleOrmSession().save(notification, VISIBILITY_STRING, userRepository.getSimpleOrmContext(user));
        return notification;
    }

    public SystemNotification createNotification(
            SystemNotificationSeverity severity,
            String title,
            String message,
            String externalUrl,
            Date startDate,
            Date endDate,
            User user
    ) {
        String actionEvent = null;
        JSONObject actionPayload = null;

        if (externalUrl != null) {
            actionEvent = Notification.ACTION_EVENT_EXTERNAL_URL;
            actionPayload = new JSONObject();
            actionPayload.put("url", externalUrl);
        }

        return createNotification(severity, title, message, actionEvent, actionPayload, startDate, endDate, user);
    }

    public SystemNotification getNotification(String rowKey, User user) {
        return getSimpleOrmSession().findById(
                SystemNotification.class,
                rowKey,
                userRepository.getSimpleOrmContext(user)
        );
    }

    public SystemNotification updateNotification(SystemNotification notification, User user) {
        getSimpleOrmSession().save(notification, VISIBILITY_STRING, userRepository.getSimpleOrmContext(user));
        return notification;
    }

    public void endNotification(SystemNotification notification, User user) {
        notification.setEndDate(new Date());
        updateNotification(notification, user);
    }

    public List<SystemNotification> getFutureNotifications(Date maxDate, User user) {
        Date now = new Date();
        List<SystemNotification> futureNotifications = new ArrayList<>();
        for (SystemNotification notification : getSimpleOrmSession().findAll(
                SystemNotification.class,
                userRepository.getSimpleOrmContext(user)
        )) {
            if (notification.getStartDate().after(now) && notification.getStartDate().before(maxDate)) {
                futureNotifications.add(notification);
            }
        }
        LOGGER.debug("returning %d future system notifications", futureNotifications.size());
        return futureNotifications;
    }
}
