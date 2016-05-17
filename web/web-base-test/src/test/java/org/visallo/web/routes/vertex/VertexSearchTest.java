package org.visallo.web.routes.vertex;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Vertex;
import org.visallo.core.model.search.SearchOptions;
import org.visallo.core.model.search.VertexSearchRunner;
import org.visallo.web.clientapi.model.ClientApiElementSearchResponse;
import org.visallo.web.routes.search.QueryResultsIterableSearchResultsSearchRouteTestBase;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VertexSearchTest extends QueryResultsIterableSearchResultsSearchRouteTestBase {
    private VertexSearch vertexSearch;

    @Mock
    private VertexSearchRunner vertexSearchRunner;

    @Before
    public void before() throws IOException {
        super.before();

        when(searchRepository.findSearchRunnerByUri(VertexSearchRunner.URI)).thenReturn(vertexSearchRunner);
        vertexSearch = new VertexSearch(searchRepository);
    }

    @Test
    public void testSearch() throws Exception {
        Vertex v1 = graph.addVertex("v1", visibility, authorizations);

        setParameter("q", "*");
        JSONArray filter = new JSONArray();
        setParameter("filter", filter);

        queryResultsIterableTotalHits = 1;
        queryResultsIterableElements.add(v1);

        when(vertexSearchRunner.run(argThat(new ArgumentMatcher<SearchOptions>() {
            @Override
            public boolean matches(Object o) {
                SearchOptions searchOptions = (SearchOptions) o;
                assertEquals("*", searchOptions.getRequiredParameter("q", String.class));
                assertEquals(filter.toString(), searchOptions.getRequiredParameter("filter", JSONArray.class).toString());
                assertEquals(WORKSPACE_ID, searchOptions.getWorkspaceId());
                return true;
            }
        }), eq(user), eq(authorizations))).thenReturn(results);

        ClientApiElementSearchResponse response = vertexSearch.handle(request, WORKSPACE_ID, user, authorizations);
        assertEquals(1, response.getElements().size());
        assertEquals(1, response.getItemCount());
    }
}