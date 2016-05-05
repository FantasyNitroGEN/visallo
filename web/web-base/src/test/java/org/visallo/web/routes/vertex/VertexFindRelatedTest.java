package org.visallo.web.routes.vertex;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Vertex;
import org.visallo.core.model.search.SearchOptions;
import org.visallo.core.model.search.VertexFindRelatedSearchResults;
import org.visallo.core.model.search.VertexFindRelatedSearchRunner;
import org.visallo.web.clientapi.model.ClientApiElementFindRelatedResponse;
import org.visallo.web.routes.search.SearchRouteTestBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VertexFindRelatedTest extends SearchRouteTestBase {
    private VertexFindRelated vertexFindRelated;

    @Mock
    private VertexFindRelatedSearchResults results;

    @Mock
    private VertexFindRelatedSearchRunner vertexFindRelatedSearchRunner;

    @Before
    public void before() throws IOException {
        super.before();

        when(searchRepository.findSearchRunnerByUri(VertexFindRelatedSearchRunner.URI)).thenReturn(vertexFindRelatedSearchRunner);

        vertexFindRelated = new VertexFindRelated(searchRepository);
    }

    @Test
    public void testSearchRelated() throws Exception {
        Vertex v1 = graph.prepareVertex("v1", visibility).save(authorizations);
        Vertex v2 = graph.prepareVertex("v2", visibility).save(authorizations);
        Vertex v3 = graph.prepareVertex("v3", visibility).save(authorizations);
        graph.addEdge("e1", v1, v2, "label1", visibility, authorizations);
        graph.addEdge("e2", v1, v3, "label1", visibility, authorizations);
        graph.flush();

        List<Vertex> elements = new ArrayList<>();
        elements.add(v2);
        elements.add(v3);
        when(results.getElements()).thenReturn((List) elements);
        when(results.getCount()).thenReturn(2L);

        setParameter("q", "*");
        setArrayParameter("graphVertexIds[]", new String[]{"v1"});

        when(vertexFindRelatedSearchRunner.run(argThat(new ArgumentMatcher<SearchOptions>() {
            @Override
            public boolean matches(Object o) {
                SearchOptions searchOptions = (SearchOptions) o;
                assertEquals("*", searchOptions.getRequiredParameter("q", String.class));
                String[] graphVertexIds = searchOptions.getRequiredParameter("graphVertexIds[]", String[].class);
                assertArrayEquals(new String[]{"v1"}, graphVertexIds);
                assertEquals(WORKSPACE_ID, searchOptions.getWorkspaceId());
                return true;
            }
        }), eq(user), eq(authorizations))).thenReturn(results);

        ClientApiElementFindRelatedResponse response = vertexFindRelated.handle(request, WORKSPACE_ID, user, authorizations);
        assertEquals(2, response.getElements().size());
        assertEquals(2, response.getCount());
    }
}