package org.visallo.core.model.notification;

import com.google.inject.Inject;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.json.JSONObject;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.UserRepository;
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
    private UserRepository userRepository;

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
        for (UserNotification notification : getSimpleOrmSession().findAll(UserNotification.class, getUserRepository().getSimpleOrmContext(user))) {
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
        UserNotification notification = new UserNotification(
                userId, title, message, actionEvent, actionPayload, expirationAge);
        saveNotification(notification, authUser);
        return notification;
    }

    public UserNotification createNotification(
            String userId,
            String title,
            String message,
            String externalUrl,
            ExpirationAge expirationAge,
            User authUser) {
        UserNotification notification = new UserNotification(userId, title, message, null, null, expirationAge);
        notification.setExternalUrl(externalUrl);
        saveNotification(notification, authUser);
        return notification;
    }

    private void saveNotification(UserNotification notification, User authUser) {
        getSimpleOrmSession().save(notification, VISIBILITY_STRING, getUserRepository().getSimpleOrmContext(authUser));
        workQueueRepository.pushUserNotification(notification);
    }

    public UserNotification getNotification(String notificationId, User user) {
        return getSimpleOrmSession().findById(UserNotification.class, notificationId, getUserRepository().getSimpleOrmContext(user));
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
        getSimpleOrmSession().saveMany(toSave, VISIBILITY_STRING, getUserRepository().getSimpleOrmContext(user));
    }

    /**
     * Avoid circular reference with UserRepository
     */
    private UserRepository getUserRepository() {
        if (userRepository == null) {
            userRepository = InjectHelper.getInstance(UserRepository.class);
        }
        return userRepository;
    }
}
