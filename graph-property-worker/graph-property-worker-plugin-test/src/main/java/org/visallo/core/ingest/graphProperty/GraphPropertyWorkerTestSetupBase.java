package org.visallo.core.ingest.graphProperty;

import org.junit.Before;
import org.mockito.Mock;
import org.vertexium.Authorizations;
import org.vertexium.Visibility;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.GraphAuthorizationRepository;
import org.visallo.core.model.user.InMemoryGraphAuthorizationRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.DirectVisibilityTranslator;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.test.GraphPropertyWorkerTestBase;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.util.*;

import static org.mockito.Mockito.when;

/**
 * This base class provides a common test setup for unit tests of GraphPropertyWorker subclasses. Both Mockito and
 * in-memory implementations are used to supply dependent objects to the GraphPropertyWorker.
 * <p/>
 * TODO: It might make sense to combine this with {@link GraphPropertyWorkerTestBase}.
 * TODO: There are a number of GPW unit tests with copied/pasted setup that should extend this base class.
 */
public abstract class GraphPropertyWorkerTestSetupBase {
    protected static final String WORKSPACE_ID = "TEST_WORKSPACE";
    protected static final String VISIBILITY_SOURCE = "TEST_VISIBILITY_SOURCE";
    protected Authorizations termMentionAuthorizations;

    @Mock
    protected User user;
    @Mock
    protected OntologyRepository ontologyRepository;
    @Mock
    protected WorkspaceRepository workspaceRepository;
    @Mock
    protected WorkQueueRepository workQueueRepository;
    protected GraphAuthorizationRepository graphAuthorizationRepository;
    protected TermMentionRepository termMentionRepository;
    protected Map<String, String> configuration = new HashMap<>();
    protected InMemoryAuthorizations authorizations;
    protected InMemoryGraph graph;
    protected Visibility visibility;
    protected VisibilityJson visibilityJson;
    protected VisibilityTranslator visibilityTranslator;
    protected GraphPropertyWorker worker; // test subject

    @Before
    public void setup() throws Exception {
        configuration.put("ontology.intent.concept.person", "http://visallo.org/test#person");
        configuration.put("ontology.intent.concept.location", "http://visallo.org/test#location");
        configuration.put("ontology.intent.concept.organization", "http://visallo.org/test#organization");
        configuration.put("ontology.intent.concept.phoneNumber", "http://visallo.org/test#phoneNumber");
        configuration.put("ontology.intent.relationship.artifactHasEntity", "http://visallo.org/test#artifactHasEntity");

        when(ontologyRepository.getRequiredConceptIRIByIntent("location")).thenReturn("http://visallo.org/test#location");
        when(ontologyRepository.getRequiredConceptIRIByIntent("organization")).thenReturn("http://visallo.org/test#organization");
        when(ontologyRepository.getRequiredConceptIRIByIntent("person")).thenReturn("http://visallo.org/test#person");
        when(ontologyRepository.getRequiredConceptIRIByIntent("phoneNumber")).thenReturn("http://visallo.org/test#phoneNumber");
        when(ontologyRepository.getRequiredRelationshipIRIByIntent("artifactHasEntity")).thenReturn("http://visallo.org/test#artifactHasEntity");

        when(user.getUserId()).thenReturn("USER123");

        List<TermMentionFilter> termMentionFilters = new ArrayList<>();
        authorizations = new InMemoryAuthorizations(VISIBILITY_SOURCE);
        configuration.putAll(getAdditionalConfiguration());
        GraphPropertyWorkerPrepareData workerPrepareData = new GraphPropertyWorkerPrepareData(
                configuration,
                termMentionFilters,
                user,
                authorizations,
                null
        );
        graph = InMemoryGraph.create();
        termMentionAuthorizations = graph.createAuthorizations(TermMentionRepository.VISIBILITY_STRING, VISIBILITY_SOURCE, WORKSPACE_ID);
        visibility = new Visibility(VISIBILITY_SOURCE);
        visibilityJson = new VisibilityJson();
        visibilityJson.setSource(VISIBILITY_SOURCE);
        visibilityJson.addWorkspace(WORKSPACE_ID);
        visibilityTranslator = new DirectVisibilityTranslator();
        graphAuthorizationRepository = new InMemoryGraphAuthorizationRepository();
        termMentionRepository = new TermMentionRepository(graph, graphAuthorizationRepository);

        worker = createGraphPropertyWorker();
        worker.setVisibilityTranslator(visibilityTranslator);
        worker.setConfiguration(new HashMapConfigurationLoader(configuration).createConfiguration());
        worker.setOntologyRepository(ontologyRepository);
        worker.setWorkspaceRepository(workspaceRepository);
        worker.setWorkQueueRepository(workQueueRepository);
        worker.prepare(workerPrepareData);
        worker.setGraph(graph);
    }

    protected abstract GraphPropertyWorker createGraphPropertyWorker();

    protected Map<String, String> getAdditionalConfiguration() {
        return Collections.emptyMap();
    }
}
