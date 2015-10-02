package org.visallo.web.routes.vertex;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.visallo.web.RouteTestBase;
import org.visallo.web.clientapi.model.ClientApiVertexSearchResponse;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VertexSearchTest extends RouteTestBase {
    private VertexSearch vertexSearch;
    private Visibility visibility;
    private Authorizations authorizations;

    @Before
    public void setUp() throws IOException {
        super.setUp();

        visibility = new Visibility("");
        authorizations = graph.createAuthorizations("");

        vertexSearch = new VertexSearch(ontologyRepository, graph, configuration);
    }

    @Test
    public void testSearchRelated() throws Exception {
        Vertex v1 = graph.addVertex("v1", visibility, authorizations);
        Vertex v2 = graph.addVertex("v2", visibility, authorizations);
        Vertex v3 = graph.addVertex("v3", visibility, authorizations);
        graph.addEdge("e1", v1, v2, "label1", visibility, authorizations);
        graph.addEdge("e2", v1, v3, "label1", visibility, authorizations);
        graph.flush();

        setArrayParameter("relatedToVertexIds[]", new String[]{"v1"});
        setParameter("filter", new JSONArray());

        when(userRepository.getAuthorizations(eq(user), eq(WORKSPACE_ID))).thenReturn(authorizations);

        ClientApiVertexSearchResponse response = vertexSearch.handle(request, WORKSPACE_ID, authorizations);
        assertEquals(2, response.getVertices().size());
        assertEquals(2, response.getItemCount());
    }
}