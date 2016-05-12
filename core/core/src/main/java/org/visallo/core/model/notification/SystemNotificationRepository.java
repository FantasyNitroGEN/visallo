package org.visallo.core.model.notification;

import com.google.inject.Inject;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.apache.commons.lang.time.DateUtils;
import org.json.JSONObject;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.lock.LeaderListener;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SystemNotificationRepository extends NotificationRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(SystemNotificationRepository.class);
    private static final Integer CHECK_INTERVAL_SECONDS_DEFAULT = 60;
    private static final String CHECK_INTERVAL_CONFIG_NAME = SystemNotificationRepository.class.getName() + ".checkIntervalSeconds";
    private static final String LOCK_NAME = SystemNotificationRepository.class.getName();
    private static final String VISIBILITY_STRING = "";
    private final UserRepository userRepository;
    private final int checkIntervalSeconds;
    private volatile boolean enabled;

    @Inject
    public SystemNotificationRepository(
            Configuration configuration,
            SimpleOrmSession simpleOrmSession,
            LockRepository lockRepository,
            UserRepository userRepository,
            WorkQueueRepository workQueueRepository
    ) {
        super(simpleOrmSession);
        this.checkIntervalSeconds = configuration.getInt(CHECK_INTERVAL_CONFIG_NAME, CHECK_INTERVAL_SECONDS_DEFAULT);
        this.userRepository = userRepository;
        if (this.checkIntervalSeconds > 0) {
            startBackgroundThread(lockRepository, userRepository, workQueueRepository);
        }
    }

    public List<SystemNotification> getActiveNotifications(User user) {
        Date now = new Date();
        List<SystemNotification> activeNotifications = new ArrayList<>();
        for (SystemNotification notification : getSimpleOrmSession().findAll(SystemNotification.class, userRepository.getSimpleOrmContext(user))) {
            if (notification.getStartDate().before(now)) {
                if (notification.getEndDate() == null || notification.getEndDate().after(now)) {
                    activeNotifications.add(notification);
                }
            }
        }
        LOGGER.debug("returning %d active system notifications", activeNotifications.size());
        return activeNotifications;
    }

    public List<SystemNotification> getFutureNotifications(Date maxDate, User user) {
        Date now = new Date();
        List<SystemNotification> futureNotifications = new ArrayList<>();
        for (SystemNotification notification : getSimpleOrmSession().findAll(SystemNotification.class, userRepository.getSimpleOrmContext(user))) {
            if (notification.getStartDate().after(now) && notification.getStartDate().before(maxDate)) {
                futureNotifications.add(notification);
            }
        }
        LOGGER.debug("returning %d future system notifications", futureNotifications.size());
        return futureNotifications;
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
        return getSimpleOrmSession().findById(SystemNotification.class, rowKey, userRepository.getSimpleOrmContext(user));
    }

    public SystemNotification updateNotification(SystemNotification notification, User user) {
        getSimpleOrmSession().save(notification, VISIBILITY_STRING, userRepository.getSimpleOrmContext(user));
        return notification;
    }

    public void endNotification(SystemNotification notification, User user) {
        notification.setEndDate(new Date());
        updateNotification(notification, user);
    }

    protected void startBackgroundThread(final LockRepository lockRepository, final UserRepository userRepository, final WorkQueueRepository workQueueRepository) {
        Thread t = new Thread(() -> {
            enabled = false;
            lockRepository.leaderElection(LOCK_NAME, new LeaderListener() {
                @Override
                public void isLeader() {
                    LOGGER.debug("using successfully acquired lock (%s)", Thread.currentThread().getName());
                    enabled = true;
                }

                @Override
                public void notLeader() {
                    LOGGER.debug("lost leadership (%s)", Thread.currentThread().getName());
                    disable();
                }
            });

            while (true) {
                try {
                    Thread.sleep(10 * 1000); // wait for enabled to change
                } catch (InterruptedException e) {
                    LOGGER.error("Failed to sleep", e);
                    throw new VisalloException("Failed to sleep", e);
                }
                runPeriodically(userRepository, workQueueRepository);
            }
        });
        t.setDaemon(true);
        t.setName(SystemNotificationRepository.class.getSimpleName() + "-background");
        t.start();
    }

    private void runPeriodically(UserRepository userRepository, WorkQueueRepository workQueueRepository) {
        try {
            while (enabled) {
                LOGGER.debug("running periodically");
                Date now = new Date();
                Date nowPlusOneMinute = DateUtils.addSeconds(now, checkIntervalSeconds);
                getFutureNotifications(nowPlusOneMinute, userRepository.getSystemUser())
                        .forEach(workQueueRepository::pushSystemNotification);
                try {
                    long remainingMilliseconds = nowPlusOneMinute.getTime() - System.currentTimeMillis();
                    if (remainingMilliseconds > 0) {
                        Thread.sleep(remainingMilliseconds);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (Throwable ex) {
            LOGGER.error("runPeriodically error", ex);
            throw ex;
        }
    }

    public void disable() {
        enabled = false;
    }
}
