package org.visallo.core.model.notification;

import com.google.inject.Inject;
import org.apache.commons.lang.time.DateUtils;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.util.PeriodicBackgroundService;

import java.util.Date;

public class SystemNotificationService extends PeriodicBackgroundService {
    private static final Integer CHECK_INTERVAL_SECONDS_DEFAULT = 60;
    private static final String CHECK_INTERVAL_CONFIG_NAME = SystemNotificationService.class.getName() + ".checkIntervalSeconds";
    private final UserRepository userRepository;
    private final Integer checkIntervalSeconds;
    private final WorkQueueRepository workQueueRepository;
    private final SystemNotificationRepository systemNotificationRepository;

    @Inject
    public SystemNotificationService(
            Configuration configuration,
            UserRepository userRepository,
            LockRepository lockRepository,
            WorkQueueRepository workQueueRepository,
            SystemNotificationRepository systemNotificationRepository
    ) {
        super(lockRepository);
        this.userRepository = userRepository;
        this.checkIntervalSeconds = configuration.getInt(CHECK_INTERVAL_CONFIG_NAME, CHECK_INTERVAL_SECONDS_DEFAULT);
        this.workQueueRepository = workQueueRepository;
        this.systemNotificationRepository = systemNotificationRepository;
    }

    @Override
    protected void run() {
        Date now = new Date();
        Date nowPlusOneMinute = DateUtils.addSeconds(now, getCheckIntervalSeconds());
        systemNotificationRepository.getFutureNotifications(nowPlusOneMinute, userRepository.getSystemUser())
                .forEach(workQueueRepository::pushSystemNotification);
    }

    @Override
    protected int getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }
}
