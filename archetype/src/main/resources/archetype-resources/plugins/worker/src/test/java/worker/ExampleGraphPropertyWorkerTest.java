#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.worker;

import org.junit.Before;
import org.junit.Test;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryAuthorizations;
import org.vertexium.inmemory.InMemoryGraph;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.query.Query;
import org.vertexium.query.SortDirection;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.DirectVisibilityTranslator;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ${package}.worker.OntologyConstants.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.visallo.core.util.StreamUtil.stream;

public class ExampleGraphPropertyWorkerTest {
    private static final String VISIBILITY_SOURCE = "TheVisibilitySource";
    private static final String WORKSPACE_ID = "WORKSPACE_ID";
    private Graph graph;
    private VisibilityTranslator visibilityTranslator;
    private WorkQueueRepository workQueueRepository;
    private WorkspaceRepository workspaceRepository;
    private User user;
    private Visibility visibility;
    private Authorizations authorizations;
    private Vertex archiveVertex;

    @Before
    public void before() throws Exception {
        graph = InMemoryGraph.create();

        visibilityTranslator = new DirectVisibilityTranslator();
        VisibilityJson visibilityJson = new VisibilityJson(VISIBILITY_SOURCE);
        visibilityJson.addWorkspace(WORKSPACE_ID);
        visibility = visibilityTranslator.toVisibility(visibilityJson).getVisibility();
        authorizations = new InMemoryAuthorizations(VISIBILITY_SOURCE, WORKSPACE_ID);

        archiveVertex = graph.addVertex(visibility, authorizations);
        VisalloProperties.MIME_TYPE.addPropertyValue(archiveVertex, "", "text/csv", visibility, authorizations);

        InputStream archiveIn = getClass().getResource("/contacts.csv").openStream();
        StreamingPropertyValue value = new StreamingPropertyValue(archiveIn, byte[].class);
        VisalloProperties.RAW.setProperty(archiveVertex, value, visibility, authorizations);
        archiveIn.close();

        user = mock(User.class);
        when(user.getUserId()).thenReturn("USER_ID");

        Workspace workspace = mock(Workspace.class);
        when(workspace.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        workspaceRepository = mock(WorkspaceRepository.class);
        when(workspaceRepository.findById(WORKSPACE_ID, user)).thenReturn(workspace);

        workQueueRepository = mock(WorkQueueRepository.class);
    }

    @Test
    public void isHandledReturnsTrueForRawPropertyWithCsvMimeType() throws Exception {
        ExampleGraphPropertyWorker worker = createWorker();

        boolean handled = worker.isHandled(archiveVertex, VisalloProperties.RAW.getProperty(archiveVertex));

        assertThat(handled, is(true));
    }

    @Test
    public void isHandledReturnsFalseForRawPropertyWithOtherMimeType() throws Exception {
        ExampleGraphPropertyWorker worker = createWorker();

        VisalloProperties.MIME_TYPE.removeProperty(archiveVertex, "", authorizations);
        VisalloProperties.MIME_TYPE.addPropertyValue(
                archiveVertex, "", "application/octet-stream", visibility, authorizations);

        boolean handled = worker.isHandled(archiveVertex, VisalloProperties.RAW.getProperty(archiveVertex));

        assertThat(handled, is(false));
    }

    @Test
    public void isHandledReturnsFalseForNullProperty() throws Exception {
        ExampleGraphPropertyWorker worker = createWorker();

        assertThat(worker.isHandled(archiveVertex, null), is(false));
    }

    @Test
    public void executeShouldCreatePersonVerticesFromContactsCsvFileVertex() throws Exception {
        ExampleGraphPropertyWorker worker = createWorker();
        GraphPropertyWorkData workData = createWorkData();

        worker.execute(null, workData);

        Query csvFileQuery = graph.query(authorizations)
                .has(VisalloProperties.CONCEPT_TYPE.getPropertyName(), CONTACTS_CSV_FILE_CONCEPT_TYPE);
        List<Vertex> csvFileVertices = stream(csvFileQuery.vertices()).collect(Collectors.toList());
        assertThat(csvFileVertices.size(), is(1));
        Vertex csvFileVertex = csvFileVertices.get(0);

        Query personQuery = graph.query(authorizations)
                                 .has(VisalloProperties.CONCEPT_TYPE.getPropertyName(), PERSON_CONCEPT_TYPE)
                                 .sort(PERSON_FULL_NAME_PROPERTY.getPropertyName(), SortDirection.ASCENDING);
        List<Vertex> personVertices = stream(personQuery.vertices()).collect(Collectors.toList());
        assertThat(personVertices.size(), is(2));

        Vertex person1Vertex = personVertices.get(0);
        assertThat(PERSON_FULL_NAME_PROPERTY.getPropertyValue(person1Vertex), is("Bruce Wayne"));
        assertThat(PERSON_EMAIL_ADDRESS_PROPERTY.getPropertyValue(person1Vertex), is("batman@example.org"));
        assertThat(PERSON_PHONE_NUMBER_PROPERTY.getPropertyValue(person1Vertex), is("888-555-0102"));
        assertThat(person1Vertex.getVisibility().hasAuthorization(WORKSPACE_ID), is(true));
        List<EdgeInfo> person1Edges = stream(
                person1Vertex.getEdgeInfos(Direction.IN, HAS_ENTITY_EDGE_LABEL, authorizations))
                .collect(Collectors.toList());
        assertThat(person1Edges.size(), is(1));
        assertThat(person1Edges.get(0).getVertexId(), is(csvFileVertex.getId()));

        Vertex person2Vertex = personVertices.get(1);
        assertThat(PERSON_FULL_NAME_PROPERTY.getPropertyValue(person2Vertex), is("Clark Kent"));
        assertThat(PERSON_EMAIL_ADDRESS_PROPERTY.getPropertyValue(person2Vertex), is("superman@example.org"));
        assertThat(PERSON_PHONE_NUMBER_PROPERTY.getPropertyValue(person2Vertex), is("888-555-0101"));
        assertThat(person2Vertex.getVisibility().hasAuthorization(WORKSPACE_ID), is(true));
        List<EdgeInfo> person2Edges = stream(
                person2Vertex.getEdgeInfos(Direction.IN, HAS_ENTITY_EDGE_LABEL, authorizations))
                .collect(Collectors.toList());
        assertThat(person2Edges.size(), is(1));
        assertThat(person2Edges.get(0).getVertexId(), is(csvFileVertex.getId()));
    }

    private ExampleGraphPropertyWorker createWorker() throws Exception {
        GraphPropertyWorkerPrepareData prepareData = createPrepareData();
        ExampleGraphPropertyWorker worker = new ExampleGraphPropertyWorker();
        worker.setGraph(graph);
        worker.setVisibilityTranslator(visibilityTranslator);
        worker.setWorkQueueRepository(workQueueRepository);
        worker.setWorkspaceRepository(workspaceRepository);
        worker.prepare(prepareData);
        return worker;
    }

    private GraphPropertyWorkerPrepareData createPrepareData() {
        return new GraphPropertyWorkerPrepareData(
                Collections.emptyMap(), Collections.emptyList(), user, authorizations, InjectHelper.getInjector());
    }

    private GraphPropertyWorkData createWorkData() {
        return new GraphPropertyWorkData(
                visibilityTranslator, archiveVertex, null, WORKSPACE_ID, VISIBILITY_SOURCE, Priority.NORMAL, false);
    }
}
