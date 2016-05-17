package org.visallo.web.routes.edge;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Vertex;
import org.visallo.core.model.search.EdgeSearchRunner;
import org.visallo.core.model.search.SearchOptions;
import org.visallo.web.clientapi.model.ClientApiElementSearchResponse;
import org.visallo.web.routes.search.QueryResultsIterableSearchResultsSearchRouteTestBase;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EdgeSearchTest extends QueryResultsIterableSearchResultsSearchRouteTestBase {
    private EdgeSearch edgeSearch;

    @Mock
    private EdgeSearchRunner edgeSearchRunner;

    @Before
    public void before() throws IOException {
        super.before();

        when(searchRepository.findSearchRunnerByUri(EdgeSearchRunner.URI)).thenReturn(edgeSearchRunner);

        edgeSearch = new EdgeSearch(searchRepository);
    }

    @Test
    public void testSearch() throws Exception {
        Vertex v1 = graph.addVertex("v1", visibility, authorizations);
        Vertex v2 = graph.addVertex("v2", visibility, authorizations);
        graph.addEdge("e1", v1, v2, "label", visibility, authorizations);

        setParameter("q", "*");
        JSONArray filter = new JSONArray();
        setParameter("filter", filter);

        queryResultsIterableTotalHits = 1;
        queryResultsIterableElements.add(v1);

        when(edgeSearchRunner.run(argThat(new ArgumentMatcher<SearchOptions>() {
            @Override
            public boolean matches(Object o) {
                SearchOptions searchOptions = (SearchOptions) o;
                assertEquals("*", searchOptions.getRequiredParameter("q", String.class));
                assertEquals(filter.toString(), searchOptions.getRequiredParameter("filter", JSONArray.class).toString());
                assertEquals(WORKSPACE_ID, searchOptions.getWorkspaceId());
                return true;
            }
        }), eq(user), eq(authorizations))).thenReturn(results);

        ClientApiElementSearchResponse response = edgeSearch.handle(request, WORKSPACE_ID, user, authorizations);
        assertEquals(1, response.getElements().size());
        assertEquals(1, response.getItemCount());
    }
}