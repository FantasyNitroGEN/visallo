package org.visallo.core.model.search;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Vertex;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Iterables.size;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class VertexFindRelatedSearchRunnerTest extends SearchRunnerTestBase {
    private VertexFindRelatedSearchRunner vertexFindRelatedSearchRunner;

    @Before
    public void before() {
        super.before();

        vertexFindRelatedSearchRunner = new VertexFindRelatedSearchRunner(
                graph,
                ontologyRepository
        );
    }

    @Test
    public void testSearch() throws Exception {
        Vertex v1 = graph.prepareVertex("v1", visibility).save(authorizations);
        Vertex v2 = graph.prepareVertex("v2", visibility).save(authorizations);
        Vertex v3 = graph.prepareVertex("v3", visibility).save(authorizations);
        graph.addEdge("e1", v1, v2, "label1", visibility, authorizations);
        graph.addEdge("e2", v1, v3, "label1", visibility, authorizations);
        graph.flush();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("graphVertexIds[]", new String[]{"v1"});
        parameters.put("q", "*");
        parameters.put("filter", new JSONArray());
        SearchOptions searchOptions = new SearchOptions(parameters, "workspace1");

        VertexFindRelatedSearchResults results = vertexFindRelatedSearchRunner.run(searchOptions, user, authorizations);
        assertEquals(2, size(results.getElements()));
    }
}