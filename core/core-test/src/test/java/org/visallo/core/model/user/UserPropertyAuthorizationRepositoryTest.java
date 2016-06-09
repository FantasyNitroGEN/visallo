package org.visallo.core.model.user;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Graph;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.model.notification.UserNotificationRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserPropertyAuthorizationRepositoryTest {
    private UserPropertyAuthorizationRepository userPropertyAuthorizationRepository;

    @Mock
    private User user;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Graph graph;

    @Mock
    private OntologyRepository ontologyRepository;

    @Mock
    private UserNotificationRepository userNotificationRepository;

    @Mock
    private WorkQueueRepository workQueueRepository;

    @Before
    public void before() {
        Map config = new HashMap();
        config.put(
                UserPropertyAuthorizationRepository.CONFIGURATION_PREFIX + ".defaultAuthorizations",
                "userRepositoryAuthorization1,userRepositoryAuthorization2"
        );
        Configuration configuration = new HashMapConfigurationLoader(config).createConfiguration();

        userPropertyAuthorizationRepository = new UserPropertyAuthorizationRepository(
                graph,
                ontologyRepository,
                configuration,
                userNotificationRepository,
                workQueueRepository
        );
    }

    @Test
    public void testGetAuthorizationsForNewUser() {
        HashSet<String> expected = Sets.newHashSet("userRepositoryAuthorization1", "userRepositoryAuthorization2");

        Set<String> privileges = userPropertyAuthorizationRepository.getAuthorizations(user);
        assertEquals(expected, privileges);
    }

    @Test
    public void testGetAuthorizationsForExisting() {
        String[] authorizationsArray = {"userAuthorization1", "userAuthorization2"};
        when(user.getProperty(eq(UserPropertyAuthorizationRepository.AUTHORIZATIONS_PROPERTY_IRI)))
                .thenReturn("userAuthorization1,userAuthorization2");

        Set<String> privileges = userPropertyAuthorizationRepository.getAuthorizations(user);
        assertEquals(Sets.newHashSet(authorizationsArray), privileges);
    }
}