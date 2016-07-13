package org.visallo.web.routes.vertex;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.web.routes.RouteTestBase;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.IOException;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UnresolveTermEntityTest extends RouteTestBase {
    private UnresolveTermEntity unresolveTermEntity;
    private Visibility visibility;
    private Visibility termMentionVisibility;
    private Authorizations authorizations;

    @Before
    public void before() throws IOException {
        super.before();

        visibility = new Visibility("");
        termMentionVisibility = new Visibility(TermMentionRepository.VISIBILITY_STRING);
        authorizations = graph.createAuthorizations(TermMentionRepository.VISIBILITY_STRING);

        unresolveTermEntity = new UnresolveTermEntity(termMentionRepository, graph, workspaceHelper);
    }

    @Test
    public void testHandle() throws Exception {
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

        when(userRepository.getAuthorizations(eq(user), eq(WORKSPACE_ID))).thenReturn(authorizations);
        when(termMentionRepository.findById(eq("v1tm1"), eq(authorizations))).thenReturn(v1tm1);

        unresolveTermEntity.handle("v1tm1", WORKSPACE_ID, authorizations);

        verify(workspaceHelper).unresolveTerm(eq(v1tm1), eq(authorizations));
    }
}
