package org.visallo.core.model.notification;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.v5analytics.simpleorm.SimpleOrmContext;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.json.JSONObject;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.visallo.core.util.StreamUtil.stream;

public class UserNotificationRepository extends NotificationRepository {
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

    @VisibleForTesting
    public UserNotificationRepository(
            SimpleOrmSession simpleOrmSession,
            WorkQueueRepository workQueueRepository,
            UserRepository userRepository
    ) {
        this(simpleOrmSession, workQueueRepository);
        this.userRepository = userRepository;
    }

    public Stream<UserNotification> getActiveNotifications(User user) {
        Date now = new Date();
        return findAll(user)
                .filter(notification ->
                                user.getUserId().equals(notification.getUserId())
                                        && notification.getSentDate().before(now)
                                        && notification.isActive()
                );
    }

    private Stream<UserNotification> findAll(User user) {
        SimpleOrmContext ctx = getUserRepository().getSimpleOrmContext(user);
        return stream(getSimpleOrmSession().findAll(UserNotification.class, ctx));
    }

    public Stream<UserNotification> getActiveNotificationsOlderThan(int duration, TimeUnit timeUnit, User user) {
        Date now = new Date();
        return findAll(user)
                .filter(notification -> {
                            if (!notification.isActive()) {
                                return false;
                            }
                            Date t = new Date(notification.getSentDate().getTime() + timeUnit.toMillis(duration));
                            return t.before(now);
                        }
                );
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
                userId,
                title,
                message,
                actionEvent,
                actionPayload,
                expirationAge
        );
        saveNotification(notification, authUser);
        return notification;
    }

    public UserNotification createNotification(
            String userId,
            String title,
            String message,
            String externalUrl,
            ExpirationAge expirationAge,
            User authUser
    ) {
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
        return getSimpleOrmSession().findById(
                UserNotification.class,
                notificationId,
                getUserRepository().getSimpleOrmContext(user)
        );
    }

    /**
     * This method only allows marking items read for the passed in user
     */
    public void markRead(String[] notificationIds, User user) {
        Collection<UserNotification> toSave = new ArrayList<>();
        for (String notificationId : notificationIds) {
            UserNotification notification = getNotification(notificationId, user);
            checkNotNull(notification, "Could not find notification with id " + notificationId);
            notification.setMarkedRead(true);
            toSave.add(notification);
        }
        getSimpleOrmSession().saveMany(toSave, VISIBILITY_STRING, getUserRepository().getSimpleOrmContext(user));
    }

    public void markNotified(Iterable<String> notificationIds, User user) {
        Collection<UserNotification> toSave = new ArrayList<>();
        for (String notificationId : notificationIds) {
            UserNotification notification = getNotification(notificationId, user);
            checkNotNull(notification, "Could not find notification with id " + notificationId);
            notification.setNotified(true);
            toSave.add(notification);
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
