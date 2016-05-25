package org.visallo.core.model.notification;

import com.v5analytics.simpleorm.InMemorySimpleOrmSession;
import com.v5analytics.simpleorm.SimpleOrmContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserNotificationRepositoryTest {
    private InMemorySimpleOrmSession simpleOrmSession;
    private UserNotificationRepository userNotificationRepository;

    @Mock
    private WorkQueueRepository workQueueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpleOrmContext simpleOrmContext;

    @Mock
    private User user1;

    @Mock
    private User user2;

    @Before
    public void before() {
        simpleOrmSession = new InMemorySimpleOrmSession();
        userNotificationRepository = new UserNotificationRepository(
                simpleOrmSession,
                workQueueRepository,
                userRepository
        );

        when(user1.getUserId()).thenReturn("user1");
        when(user2.getUserId()).thenReturn("user2");
    }

    @Test
    public void testGetActiveNotifications() {
        simpleOrmSession.save(new UserNotification(
                "user1",
                "Expired",
                "Message 1",
                null,
                null,
                new Date(new Date().getTime() - (2 * 60 * 1000)),
                new ExpirationAge(1, ExpirationAgeUnit.MINUTE)
        ), "", simpleOrmContext);
        simpleOrmSession.save(new UserNotification(
                "user1",
                "Current",
                "Message 2",
                null,
                null,
                new Date(),
                new ExpirationAge(1, ExpirationAgeUnit.MINUTE)
        ), "", simpleOrmContext);
        simpleOrmSession.save(new UserNotification(
                "user2",
                "Other User's",
                "Message 3",
                null,
                null,
                new Date(),
                new ExpirationAge(1, ExpirationAgeUnit.MINUTE)
        ), "", simpleOrmContext);

        when(userRepository.getSimpleOrmContext(eq(user1))).thenReturn(simpleOrmContext);

        List<UserNotification> activeNotifications = userNotificationRepository.getActiveNotifications(user1)
                .collect(Collectors.toList());
        assertEquals(1, activeNotifications.size());
        assertEquals("Current", activeNotifications.get(0).getTitle());
    }

    @Test
    public void testGetActiveNotificationsInDateRange() {
        List<UserNotification> activeNotifications;
        long currentTime = new Date().getTime();

        simpleOrmSession.save(
                new UserNotification("user1", "t-120", "Message 1", null, null, new Date(currentTime - 120000), null),
                "",
                simpleOrmContext
        );
        simpleOrmSession.save(
                new UserNotification("user1", "t-60", "Message 2", null, null, new Date(currentTime - 60000), null),
                "",
                simpleOrmContext
        );
        simpleOrmSession.save(
                new UserNotification("user1", "t-30", "Message 3", null, null, new Date(currentTime - 30000), null),
                "",
                simpleOrmContext
        );

        activeNotifications = userNotificationRepository.getActiveNotificationsOlderThan(80, TimeUnit.SECONDS, user2)
                .collect(Collectors.toList());
        assertEquals(1, activeNotifications.size());
        assertEquals("t-120", activeNotifications.get(0).getTitle());

        activeNotifications = userNotificationRepository.getActiveNotificationsOlderThan(45, TimeUnit.SECONDS, user2)
                .collect(Collectors.toList());
        assertEquals(2, activeNotifications.size());
        assertEquals("t-120", activeNotifications.get(0).getTitle());
        assertEquals("t-60", activeNotifications.get(1).getTitle());

        activeNotifications = userNotificationRepository.getActiveNotificationsOlderThan(10, TimeUnit.SECONDS, user2)
                .collect(Collectors.toList());
        assertEquals(3, activeNotifications.size());
        assertEquals("t-120", activeNotifications.get(0).getTitle());
        assertEquals("t-60", activeNotifications.get(1).getTitle());
        assertEquals("t-30", activeNotifications.get(2).getTitle());
    }
}