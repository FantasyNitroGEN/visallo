package org.visallo.core.model.termMention;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.GraphAuthorizationRepository;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class TermMentionRepositoryTest {
    private static final String WORKSPACE_ID = "WORKSPACE_1234";
    private InMemoryGraph graph;
    private Visibility visibility;
    private Visibility termMentionVisibility;
    private Authorizations authorizations;
    private TermMentionRepository termMentionRepository;

    @Mock
    private GraphAuthorizationRepository graphAuthorizationRepository;

    @Before
    public void setUp() {
        graph = InMemoryGraph.create();

        visibility = new Visibility("");
        termMentionVisibility = new Visibility(TermMentionRepository.VISIBILITY_STRING);
        authorizations = graph.createAuthorizations(TermMentionRepository.VISIBILITY_STRING);

        termMentionRepository = new TermMentionRepository(graph, graphAuthorizationRepository);
    }

    @Test
    public void testDelete() {
        Vertex v1 = graph.addVertex("v1", visibility, authorizations);
        Vertex v1tm1 = graph.addVertex("v1tm1", termMentionVisibility, authorizations);
        VisalloProperties.TERM_MENTION_RESOLVED_EDGE_ID.setProperty(v1tm1, "v1_to_v2", termMentionVisibility, authorizations);
        Vertex v2 = graph.addVertex("v2", visibility, authorizations);
        graph.addEdge("v1_to_c1tm1", v1, v1tm1, VisalloProperties.TERM_MENTION_LABEL_HAS_TERM_MENTION, termMentionVisibility, authorizations);
        graph.addEdge("c1tm1_to_v2", v1tm1, v2, VisalloProperties.TERM_MENTION_LABEL_RESOLVED_TO, termMentionVisibility, authorizations);
        Edge e = graph.addEdge("v1_to_v2", v1, v2, "link", visibility, authorizations);
        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.addWorkspace(WORKSPACE_ID);
        VisalloProperties.VISIBILITY_JSON.setProperty(e, visibilityJson, new Visibility(""), authorizations);
        graph.flush();

        termMentionRepository.delete(v1tm1, authorizations);

        assertNull("term mention should not exist", graph.getVertex("v1tm1", authorizations));
        assertNull("term mention to v2 should not exist", graph.getEdge("c1tm1_to_v2", authorizations));
        assertNull("v1 to term mention should not exist", graph.getEdge("v1_to_c1tm1", authorizations));
    }

    @Test
    public void testFindResolvedToForRef() {
        Vertex v = graph.addVertex("v", visibility, authorizations);
        VertexBuilder tmBuilder = graph.prepareVertex("tm", termMentionVisibility);
        VisalloProperties.TERM_MENTION_REF_PROPERTY_KEY.setProperty(tmBuilder, "key", termMentionVisibility);
        VisalloProperties.TERM_MENTION_REF_PROPERTY_NAME.setProperty(tmBuilder, "name", termMentionVisibility);
        Vertex tm = tmBuilder.save(authorizations);
        graph.addEdge("tm_to_v", tm, v, VisalloProperties.TERM_MENTION_LABEL_RESOLVED_TO, termMentionVisibility, authorizations);
        graph.flush();

        List<Vertex> results = termMentionRepository.findResolvedToForRef(v.getId(), "key", "name", authorizations).collect(Collectors.toList());
        assertEquals(1, results.size());
        assertEquals("tm", results.get(0).getId());
    }

    @Test
    public void findResolvedToForRefElement() {
        Vertex v = graph.addVertex("v", visibility, authorizations);
        VertexBuilder tmBuilder = graph.prepareVertex("tm", termMentionVisibility);
        Vertex tm = tmBuilder.save(authorizations);
        graph.addEdge("tm_to_v", tm, v, VisalloProperties.TERM_MENTION_LABEL_RESOLVED_TO, termMentionVisibility, authorizations);
        graph.flush();

        List<Vertex> results = termMentionRepository.findResolvedToForRefElement(v.getId(), authorizations).collect(Collectors.toList());
        assertEquals(1, results.size());
        assertEquals("tm", results.get(0).getId());
    }
}