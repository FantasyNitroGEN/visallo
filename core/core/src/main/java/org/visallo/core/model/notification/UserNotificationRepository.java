package org.visallo.core.model.notification;

import com.google.inject.Inject;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class UserNotificationRepository extends NotificationRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(UserNotificationRepository.class);
    private static final String VISIBILITY_STRING = "";
    private final WorkQueueRepository workQueueRepository;

    @Inject
    public UserNotificationRepository(
            SimpleOrmSession simpleOrmSession,
            WorkQueueRepository workQueueRepository
    ) {
        super(simpleOrmSession);
        this.workQueueRepository = workQueueRepository;
    }

    public List<UserNotification> getActiveNotifications(User user) {
        Date now = new Date();
        List<UserNotification> activeNotifications = new ArrayList<>();
        for (UserNotification notification : getSimpleOrmSession().findAll(UserNotification.class, user.getSimpleOrmContext())) {
            if (user.getUserId().equals(notification.getUserId()) &&
                    notification.getSentDate().before(now) &&
                    notification.isActive()) {
                activeNotifications.add(notification);
            }
        }
        LOGGER.debug("returning %d active user notifications", activeNotifications.size());
        return activeNotifications;
    }

    public UserNotification createNotification(
            String userId,
            String title,
            String message,
            String actionEvent,
            JSONObject actionPayload,
            ExpirationAge expirationAge,
            User authUser
    ) {
        UserNotification notification = new UserNotification(userId, title, message, actionEvent, actionPayload, expirationAge);

        notification.setMarkedRead(false);
        getSimpleOrmSession().save(notification, VISIBILITY_STRING, authUser.getSimpleOrmContext());
        workQueueRepository.pushUserNotification(notification);
        return notification;
    }

    public UserNotification getNotification(String notificationId, User user) {
        return getSimpleOrmSession().findById(UserNotification.class, notificationId, user.getSimpleOrmContext());
    }

    public void markRead(String[] notificationIds, User user) {
        Collection<UserNotification> toSave = new ArrayList<>();
        for (String notificationId : notificationIds) {
            UserNotification notification = getNotification(notificationId, user);
            checkNotNull(notification, "Could not find notification with id " + notificationId);
            if (notification.getUserId().equals(user.getUserId())) {
                notification.setMarkedRead(true);
                toSave.add(notification);
            } else throw new VisalloException("User cannot mark notifications read that aren't issued to them");
        }
        getSimpleOrmSession().saveMany(toSave, VISIBILITY_STRING, user.getSimpleOrmContext());
    }
}
