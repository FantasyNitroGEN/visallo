package model.user;

import com.v5analytics.simpleorm.SimpleOrmSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.id.UUIDIdGenerator;
import org.vertexium.inmemory.InMemoryGraph;
import org.vertexium.inmemory.InMemoryGraphConfiguration;
import org.vertexium.search.DefaultSearchIndex;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.lock.NonLockingLockRepository;
import org.visallo.core.model.notification.UserNotificationRepository;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.*;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.vertexium.model.user.VertexiumUser;
import org.visallo.vertexium.model.user.VertexiumUserRepository;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VertexiumUserRepositoryTest {
    @Mock
    private OntologyRepository ontologyRepository;

    @Mock
    private Concept userConcept;

    private GraphAuthorizationRepository graphAuthorizationRepository;
    private VertexiumUserRepository vertexiumUserRepository;

    @Mock
    private SimpleOrmSession simpleOrmSession;

    @Mock
    private UserSessionCounterRepository userSessionCounterRepository;

    @Mock
    private WorkQueueRepository workQueueRepository;

    @Mock
    private UserNotificationRepository userNotificationRepository;

    private LockRepository lockRepository = new NonLockingLockRepository();

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Mock
    private PrivilegeRepository privilegeRepository;

    @Before
    public void setup() {
        InMemoryGraphConfiguration config = new InMemoryGraphConfiguration(new HashMap());
        graphAuthorizationRepository = new InMemoryGraphAuthorizationRepository();
        graphAuthorizationRepository.addAuthorizationToGraph(VisalloVisibility.SUPER_USER_VISIBILITY_STRING.toString());
        when(ontologyRepository.getOrCreateConcept(
                (Concept) isNull(),
                eq(UserRepository.USER_CONCEPT_IRI),
                anyString(),
                anyObject(),
                anyBoolean()
        )).thenReturn(userConcept);
        when(userConcept.getIRI()).thenReturn(UserRepository.USER_CONCEPT_IRI);

        Configuration visalloConfiguration = new HashMapConfigurationLoader(new HashMap()).createConfiguration();
        vertexiumUserRepository = new VertexiumUserRepository(
                visalloConfiguration,
                simpleOrmSession,
                graphAuthorizationRepository,
                InMemoryGraph.create(config, new UUIDIdGenerator(config), new DefaultSearchIndex(config)),
                ontologyRepository,
                userSessionCounterRepository,
                workQueueRepository,
                lockRepository,
                authorizationRepository,
                privilegeRepository
        );
    }

    @Test
    public void testFindOrAddUser() {
        vertexiumUserRepository.findOrAddUser("12345", "testUser", null, "testPassword");

        VertexiumUser vertexiumUser = (VertexiumUser) vertexiumUserRepository.findByUsername("12345");
        assertEquals("testUser", vertexiumUser.getDisplayName());
    }
}
