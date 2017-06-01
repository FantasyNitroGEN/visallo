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
public class EdgeSearchRunnerTest extends SearchRunnerTestBase {
    private EdgeSearchRunner edgeSearchRunner;

    @Before
    public void before() {
        super.before();

        edgeSearchRunner = new EdgeSearchRunner(
                ontologyRepository,
                graph,
                configuration,
                directoryRepository
        );
    }

    @Test
    public void testSearch() throws Exception {
        Vertex v1 = graph.addVertex("v1", visibility, authorizations);
        Vertex v2 = graph.addVertex("v2", visibility, authorizations);
        Vertex v3 = graph.addVertex("v3", visibility, authorizations);
        graph.prepareEdge("e1", v1, v2, "label1", visibility)
                .addPropertyValue("k1", "name", "Joe", visibility)
                .save(authorizations);
        graph.prepareEdge("e2", v1, v3, "label1", visibility)
                .addPropertyValue("k1", "name", "Bob", visibility)
                .save(authorizations);
        graph.flush();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("q", "*");
        parameters.put("filter", new JSONArray());
        SearchOptions searchOptions = new SearchOptions(parameters, "workspace1");

        QueryResultsIterableSearchResults results = edgeSearchRunner.run(searchOptions, user, authorizations);
        assertEquals(2, size(results.getVertexiumObjects()));
    }
}