package org.visallo.core.model.user;

import com.google.common.collect.ImmutableMap;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.apache.commons.lang.SerializationUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.*;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * These tests are mainly concerned with verifying interaction with the Apache Curator Framework under
 * various conditions.
 */
@RunWith(MockitoJUnitRunner.class)
public class UserSessionCounterRepositoryTest {
    private static final String BASE_PATH = "/sessions";
    private static final String USER_ID = "TheUser";
    private static final String SESSION_ID = "TheSession";
    private static final String USER_PATH = BASE_PATH + "/" + USER_ID;
    private static final String SESSION_PATH = USER_PATH + "/" + SESSION_ID;

    @Mock
    private CuratorFramework curator;
    @Mock
    private ExistsBuilder existsBuilder;
    @Mock
    private ProtectACLCreateModePathAndBytesable<String> parentPathBuilder;
    @Mock
    private CreateBuilder createBuilder;
    @Mock
    private DeleteBuilder deleteBuilder;
    @Mock
    private Pathable<Void> backgroundDelete;
    @Mock
    private SetDataBuilder setDataBuilder;
    @Mock
    private GetDataBuilder getDataBuilder;
    @Mock
    private GetChildrenBuilder getChildrenBuilder;

    private Configuration configuration;
    private UserSessionCounterRepository uscRepository;

    @Before
    @SuppressWarnings("unchecked")
    public void before() throws Exception {
        configuration = new HashMapConfigurationLoader(ImmutableMap.of(
                Configuration.USER_SESSION_COUNTER_PATH_PREFIX, BASE_PATH
        )).createConfiguration();

        setUpMocks();
        uscRepository = new UserSessionCounterRepository(curator, configuration, false);

        // Reset mocks invoked in the above constructor
        reset(curator, createBuilder, parentPathBuilder);
        setUpMocks();
    }

    private void setUpMocks() {
        when(curator.checkExists()).thenReturn(existsBuilder);
        when(curator.create()).thenReturn(createBuilder);
        when(createBuilder.creatingParentsIfNeeded()).thenReturn(parentPathBuilder);
        when(curator.delete()).thenReturn(deleteBuilder);
        when(deleteBuilder.inBackground()).thenReturn(backgroundDelete);
        when(curator.setData()).thenReturn(setDataBuilder);
        when(curator.getData()).thenReturn(getDataBuilder);
        when(curator.getChildren()).thenReturn(getChildrenBuilder);
    }

    @Test
    public void newUserSessionCounterRepositoryShouldCreateBasePath() throws Exception {
        new UserSessionCounterRepository(curator, configuration, false);
        verify(parentPathBuilder).forPath(BASE_PATH);
        verifyNoMoreInteractions(parentPathBuilder);
    }

    @Test
    public void updateSessionShouldInvokeCreateForNewSession() throws Exception {
        final int expectedSessionCount = 1;

        when(existsBuilder.forPath(SESSION_PATH)).thenReturn(null);
        when(existsBuilder.forPath(USER_PATH)).thenReturn(newStat(expectedSessionCount));

        int sessionCount = uscRepository.updateSession(USER_ID, SESSION_ID, false);

        verifyZeroInteractions(deleteBuilder);
        verify(createBuilder).creatingParentsIfNeeded();
        verify(parentPathBuilder).forPath(USER_PATH);
        verify(createBuilder).forPath(SESSION_PATH);
        verify(setDataBuilder).forPath(eq(SESSION_PATH), any(byte[].class));
        verifyNoMoreInteractions(parentPathBuilder);
        verifyNoMoreInteractions(createBuilder);
        verifyNoMoreInteractions(setDataBuilder);
        assertEquals(expectedSessionCount, sessionCount);
    }

    @Test
    public void updateSessionShouldInvokeSetDataForSessionLastCountedLongTimeAgo() throws Exception {
        final int expectedSessionCount = 1;

        when(existsBuilder.forPath(SESSION_PATH)).thenReturn(newStat(0, longTimeAgo()));
        when(existsBuilder.forPath(USER_PATH)).thenReturn(newStat(expectedSessionCount));

        int sessionCount = uscRepository.updateSession(USER_ID, SESSION_ID, false);

        verifyZeroInteractions(createBuilder);
        verifyZeroInteractions(deleteBuilder);
        verify(setDataBuilder).forPath(eq(SESSION_PATH), any(byte[].class));
        verifyNoMoreInteractions(setDataBuilder);
        assertEquals(expectedSessionCount, sessionCount);
    }

    @Test
    public void updateSessionShouldInvokeNothingForSessionLastCountedShortTimeAgo() throws Exception {
        final int expectedSessionCount = 1;

        when(existsBuilder.forPath(SESSION_PATH)).thenReturn(newStat(0, shortTimeAgo()));
        when(existsBuilder.forPath(USER_PATH)).thenReturn(newStat(expectedSessionCount));

        int sessionCount = uscRepository.updateSession(USER_ID, SESSION_ID, false);

        verifyZeroInteractions(createBuilder);
        verifyZeroInteractions(deleteBuilder);
        verifyZeroInteractions(setDataBuilder);
        assertEquals(expectedSessionCount, sessionCount);
    }

    @Test
    public void deleteSessionShouldInvokeDeleteForExistingSession() throws Exception {
        final int expectedSessionCount = 1;

        when(existsBuilder.forPath(SESSION_PATH)).thenReturn(newStat());
        when(existsBuilder.forPath(USER_PATH)).thenReturn(newStat(expectedSessionCount));

        int sessionCount = uscRepository.deleteSession(USER_ID, SESSION_ID);

        verifyZeroInteractions(createBuilder);
        verifyZeroInteractions(setDataBuilder);
        verify(deleteBuilder).forPath(SESSION_PATH);
        verifyNoMoreInteractions(deleteBuilder);
        assertEquals(expectedSessionCount, sessionCount);
    }

    @Test
    public void deleteSessionShouldInvokeDeleteForExistingLastSessionAndThenInvokeDeleteUser() throws Exception {
        final int expectedSessionCount = 0;

        when(existsBuilder.forPath(SESSION_PATH)).thenReturn(newStat());
        when(existsBuilder.forPath(USER_PATH)).thenReturn(newStat(expectedSessionCount));

        int sessionCount = uscRepository.deleteSession(USER_ID, SESSION_ID);

        verifyZeroInteractions(createBuilder);
        verifyZeroInteractions(setDataBuilder);
        verify(deleteBuilder).forPath(SESSION_PATH);
        verify(deleteBuilder).inBackground();
        verify(backgroundDelete).forPath(USER_PATH);
        verifyNoMoreInteractions(deleteBuilder);
        verifyNoMoreInteractions(backgroundDelete);
        assertEquals(expectedSessionCount, sessionCount);
    }

    @Test
    public void deleteOldSessionsShouldInvokeDeleteOnlyOnOldSessions() throws Exception {
        String[] userIds = new String[] { USER_ID, USER_ID + "X" };
        String[] userPaths = new String[] { USER_PATH, USER_PATH + "X" };
        String[] sessionIds = new String[] { SESSION_ID, SESSION_ID + "X", SESSION_ID + "Y" };
        String[] sessionPaths = new String[] { SESSION_PATH, SESSION_PATH + "X", SESSION_PATH + "Y" };
        when(getChildrenBuilder.forPath(BASE_PATH)).thenReturn(Arrays.asList(userIds));
        when(getChildrenBuilder.forPath(userPaths[0])).thenReturn(Arrays.asList(sessionIds));
        when(getChildrenBuilder.forPath(userPaths[1])).thenReturn(Collections.<String>emptyList());
        when(existsBuilder.forPath(sessionPaths[0])).thenReturn(newStat(0, reallyLongTimeAgo()));
        when(existsBuilder.forPath(sessionPaths[1])).thenReturn(newStat(0, reallyLongTimeAgo()));
        when(existsBuilder.forPath(sessionPaths[2])).thenReturn(newStat(0, longTimeAgo()));
        when(getDataBuilder.forPath(sessionPaths[0])).thenReturn(SerializationUtils.serialize(new UserSessionCounterRepository.SessionData(true)));
        when(getDataBuilder.forPath(sessionPaths[1])).thenReturn(SerializationUtils.serialize(new UserSessionCounterRepository.SessionData(false)));

        uscRepository.deleteOldSessions();

        verifyZeroInteractions(createBuilder);
        verifyZeroInteractions(setDataBuilder);
        verify(deleteBuilder).inBackground();
        verify(backgroundDelete).forPath(sessionPaths[0]); // the only session deleted!
        verifyNoMoreInteractions(deleteBuilder);
        verifyNoMoreInteractions(backgroundDelete);
    }

    private Stat newStat(int numChildren, long modTime) {
        Stat stat = new Stat();
        stat.setNumChildren(numChildren);
        stat.setMtime(modTime);
        return stat;
    }

    private Stat newStat(int numChildren) {
        return newStat(numChildren, 0);
    }

    private Stat newStat() {
        return newStat(0, 0);
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private long reallyLongTimeAgo() {
        return now() - UserSessionCounterRepository.UNSEEN_SESSION_DURATION - 10;
    }

    private long longTimeAgo() {
        return now() - UserSessionCounterRepository.SESSION_UPDATE_DURATION - 10;
    }

    private long shortTimeAgo() {
        return now() - UserSessionCounterRepository.SESSION_UPDATE_DURATION + 10;
    }
}
