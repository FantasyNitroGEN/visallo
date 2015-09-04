package org.visallo.core.model.workspace;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.web.clientapi.model.VisibilityJson;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceHelperTest {
    private static final String WORKSPACE_ID = "WORKSPACE_1234";
    private InMemoryGraph graph;
    private Visibility visibility;
    private Visibility termMentionVisibility;
    private Authorizations authorizations;
    private WorkspaceHelper workspaceHelper;

    @Mock
    private TermMentionRepository termMentionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkQueueRepository workQueueRepository;

    @Mock
    private OntologyRepository ontologyRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Before
    public void setUp() {
        graph = InMemoryGraph.create();

        visibility = new Visibility("");
        termMentionVisibility = new Visibility(TermMentionRepository.VISIBILITY_STRING);
        authorizations = graph.createAuthorizations(TermMentionRepository.VISIBILITY_STRING);

        when(ontologyRepository.getRelationshipIRIByIntent(eq("entityHasImage"))).thenReturn("http://visallo.org/test#entityHasImage");
        when(ontologyRepository.getRelationshipIRIByIntent(eq("artifactContainsImageOfEntity"))).thenReturn("http://visallo.org/test#artifactContainsImageOfEntity");
        workspaceHelper = new WorkspaceHelper(termMentionRepository, userRepository, workQueueRepository, graph, ontologyRepository, workspaceRepository);
    }

    @Test
    public void testUnresolveTerm() throws Exception {
        Vertex v1 = graph.addVertex("v1", visibility, authorizations);
        Vertex v1tm1 = graph.addVertex("v1tm1", termMentionVisibility, authorizations);
        VisalloProperties.TERM_MENTION_RESOLVED_EDGE_ID.setProperty(v1tm1, "v1_to_v2", termMentionVisibility, authorizations);
        Vertex v2 = graph.addVertex("v2", visibility, authorizations);
        graph.addEdge("v1_to_c1tm1", v1, v1tm1, VisalloProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, termMentionVisibility, authorizations);
        graph.addEdge("c1tm1_to_v2", v1tm1, v2, VisalloProperties.TERM_MENTION_LABEL_RESOLVED_TO, termMentionVisibility, authorizations);
        Edge e = graph.addEdge("v1_to_v2", v1, v2, "link", visibility, authorizations);
        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.addWorkspace(WORKSPACE_ID);
        VisalloProperties.VISIBILITY_JSON.setProperty(e, visibilityJson, visibility, authorizations);
        graph.flush();

        when(termMentionRepository.findSourceVertex(v1tm1, authorizations)).thenReturn(v1);

        workspaceHelper.unresolveTerm(v1tm1, authorizations);

        verify(termMentionRepository).delete(eq(v1tm1), eq(authorizations));
    }
}