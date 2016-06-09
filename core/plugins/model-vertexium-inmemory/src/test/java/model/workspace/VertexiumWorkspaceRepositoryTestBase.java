package model.workspace;

import com.google.inject.Injector;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.junit.After;
import org.mockito.Mock;
import org.vertexium.Metadata;
import org.vertexium.Vertex;
import org.vertexium.VertexiumException;
import org.vertexium.Visibility;
import org.vertexium.id.IdGenerator;
import org.vertexium.id.QueueIdGenerator;
import org.vertexium.id.UUIDIdGenerator;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.vertexium.inmemory.InMemoryGraph;
import org.vertexium.inmemory.InMemoryGraphConfiguration;
import org.vertexium.search.DefaultSearchIndex;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.formula.FormulaEvaluator;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.lock.NonLockingLockRepository;
import org.visallo.core.model.notification.UserNotificationRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.*;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceDiffHelper;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceUndoHelper;
import org.visallo.core.security.DirectVisibilityTranslator;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.vertexium.model.ontology.InMemoryOntologyProperty;
import org.visallo.vertexium.model.user.InMemoryUser;
import org.visallo.vertexium.model.user.InMemoryUserRepository;
import org.visallo.vertexium.model.workspace.VertexiumWorkspaceRepository;
import org.visallo.web.clientapi.model.GraphPosition;

import java.util.HashMap;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public abstract class VertexiumWorkspaceRepositoryTestBase {
    protected static final VisibilityTranslator VISIBILITY_TRANSLATOR = new DirectVisibilityTranslator();
    protected static final Visibility DEFAULT_VISIBILITY = VISIBILITY_TRANSLATOR.getDefaultVisibility();
    protected static final InMemoryAuthorizations NO_AUTHORIZATIONS = new InMemoryAuthorizations();
    protected static final GraphPosition GRAPH_POSITION = new GraphPosition(100, 100);
    protected static final String PROP1_IRI = "prop1";

    protected InMemoryGraph graph;
    protected InMemoryUser user1;
    protected InMemoryUser user2;
    protected QueueIdGenerator idGenerator;
    protected Vertex entity1Vertex;
    protected VertexiumWorkspaceRepository workspaceRepository;
    protected WorkspaceHelper workspaceHelper;
    protected WorkspaceUndoHelper workspaceUndoHelper;
    protected GraphAuthorizationRepository graphAuthorizationRepository;
    protected GraphRepository graphRepository;
    protected UserRepository userRepository;

    @Mock
    protected TermMentionRepository termMentionRepository;
    @Mock
    protected SimpleOrmSession simpleOrmSession;
    @Mock
    protected UserSessionCounterRepository userSessionCounterRepository;
    @Mock
    protected WorkQueueRepository workQueueRepository;
    @Mock
    protected UserNotificationRepository userNotificationRepository;
    @Mock
    protected OntologyRepository ontologyRepository;
    @Mock
    protected FormulaEvaluator formulaEvaluator;
    @Mock
    protected Injector injector;

    protected UserPropertyAuthorizationRepository authorizationRepository;
    protected UserPropertyPrivilegeRepository privilegeRepository;

    protected static class QueueUuidFallbackIdGenerator extends QueueIdGenerator {
        private IdGenerator fallbackIdGenerator = new UUIDIdGenerator(null);

        @Override
        public String nextId() {
            try {
                return super.nextId();
            } catch (VertexiumException e) {
                return fallbackIdGenerator.nextId();
            }
        }
    }

    public void before() throws Exception {
        initMocks(this);
        InjectHelper.setInjector(injector);

        InMemoryGraphConfiguration config = new InMemoryGraphConfiguration(new HashMap<>());
        idGenerator = new VertexiumWorkspaceRepositoryTest.QueueUuidFallbackIdGenerator();
        graph = InMemoryGraph.create(config, idGenerator, new DefaultSearchIndex(config));
        graphAuthorizationRepository = new InMemoryGraphAuthorizationRepository();

        HashMap configMap = new HashMap();
        configMap.put("org.visallo.core.model.user.UserPropertyAuthorizationRepository.defaultAuthorizations", "");
        Configuration visalloConfiguration = new HashMapConfigurationLoader(configMap).createConfiguration();
        LockRepository lockRepository = new NonLockingLockRepository();

        authorizationRepository = new UserPropertyAuthorizationRepository(
                graph,
                ontologyRepository,
                visalloConfiguration,
                userNotificationRepository,
                workQueueRepository
        ) {
            @Override
            protected UserRepository getUserRepository() {
                return userRepository;
            }
        };

        privilegeRepository = new UserPropertyPrivilegeRepository(
                ontologyRepository,
                visalloConfiguration,
                userNotificationRepository,
                workQueueRepository
        ) {
            @Override
            protected UserRepository getUserRepository() {
                return userRepository;
            }
        };

        userRepository = new InMemoryUserRepository(
                visalloConfiguration,
                simpleOrmSession,
                userSessionCounterRepository,
                workQueueRepository,
                lockRepository,
                authorizationRepository,
                privilegeRepository
        );

        user1 = (InMemoryUser) userRepository.findOrAddUser("user1", "user1", null, "none");
        graph.addVertex(user1.getUserId(), DEFAULT_VISIBILITY, NO_AUTHORIZATIONS);

        user2 = (InMemoryUser) userRepository.findOrAddUser("user2", "user2", null, "none");
        graph.addVertex(user2.getUserId(), DEFAULT_VISIBILITY, NO_AUTHORIZATIONS);

        WorkspaceDiffHelper workspaceDiff = new WorkspaceDiffHelper(
                graph,
                userRepository,
                authorizationRepository,
                formulaEvaluator
        );

        workspaceRepository = new VertexiumWorkspaceRepository(
                graph,
                userRepository,
                graphAuthorizationRepository,
                workspaceDiff,
                lockRepository,
                VISIBILITY_TRANSLATOR,
                termMentionRepository,
                ontologyRepository,
                workQueueRepository,
                authorizationRepository
        );

        workspaceHelper = new WorkspaceHelper(
                termMentionRepository,
                workQueueRepository,
                graph,
                ontologyRepository,
                workspaceRepository,
                privilegeRepository,
                authorizationRepository
        );

        workspaceUndoHelper = new WorkspaceUndoHelper(
                graph,
                workspaceHelper,
                workQueueRepository
        );

        graphRepository = new GraphRepository(
                graph,
                VISIBILITY_TRANSLATOR,
                termMentionRepository
        );

        InMemoryOntologyProperty prop1 = new InMemoryOntologyProperty();
        prop1.setUserVisible(true);
        when(ontologyRepository.getPropertyByIRI(PROP1_IRI)).thenReturn(prop1);

        String entity1VertexId = "entity1Id";
        entity1Vertex = graph.prepareVertex(entity1VertexId, DEFAULT_VISIBILITY)
                .addPropertyValue("key1", "prop1", "value1", new Metadata(), DEFAULT_VISIBILITY)
                .addPropertyValue("key9", "prop9", "value9", new Metadata(), DEFAULT_VISIBILITY)
                .save(NO_AUTHORIZATIONS);
    }

    @After
    public void teardown() {
        InjectHelper.setInjector(null);
    }
}
