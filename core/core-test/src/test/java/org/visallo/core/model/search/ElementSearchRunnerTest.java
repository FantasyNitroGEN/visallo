package org.visallo.core.model.search;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Element;
import org.vertexium.Vertex;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.collect.Iterables.size;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ElementSearchRunnerTest extends SearchRunnerTestBase {
    private ElementSearchRunner elementSearchRunner;

    @Before
    public void before() {
        super.before();

        elementSearchRunner = new ElementSearchRunner(
                ontologyRepository,
                graph,
                configuration,
                directoryRepository
        );
    }

    @Test
    public void testSearch() throws Exception {
        Vertex v1 = graph.prepareVertex("v1", visibility)
                .addPropertyValue("k1", "name", "Tom", visibility)
                .save(authorizations);
        Vertex v2 = graph.prepareVertex("v2", visibility)
                .addPropertyValue("k1", "name", "Jack", visibility)
                .save(authorizations);
        Vertex v3 = graph.prepareVertex("v3", visibility)
                .addPropertyValue("k1", "name", "Phil", visibility)
                .save(authorizations);
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

        QueryResultsIterableSearchResults results = elementSearchRunner.run(searchOptions, user, authorizations);
        assertEquals(5, size(results.getElements()));
    }

    @Test
    public void testSortWithStringArray() throws Exception {
        Vertex v1 = graph.prepareVertex("v1", visibility)
                .addPropertyValue("k1", "name", "B", visibility)
                .save(authorizations);
        Vertex v2 = graph.prepareVertex("v2", visibility)
                .addPropertyValue("k1", "name", "A", visibility)
                .save(authorizations);
        graph.flush();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("q", "*");
        parameters.put("filter", new JSONArray());
        parameters.put("sort[]", new String[] { "name:ASCENDING" });
        SearchOptions searchOptions = new SearchOptions(parameters, "workspace1");

        QueryResultsIterableSearchResults results = elementSearchRunner.run(searchOptions, user, authorizations);
        assertEquals(2, size(results.getElements()));
        Iterator<? extends Element> elements = results.getElements().iterator();

        Element first = elements.next();
        assertEquals("A", first.getProperty("name").getValue());
        Element second = elements.next();
        assertEquals("B", second.getProperty("name").getValue());
    }

    @Test
    public void testSortWithJsonArray() throws Exception {
        Vertex v1 = graph.prepareVertex("v1", visibility)
                .addPropertyValue("k1", "name", "A", visibility)
                .save(authorizations);
        Vertex v2 = graph.prepareVertex("v2", visibility)
                .addPropertyValue("k1", "name", "B", visibility)
                .save(authorizations);
        graph.flush();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("q", "*");
        parameters.put("filter", new JSONArray());
        JSONArray sorts = new JSONArray(new String[] { "name:DESCENDING"});
        parameters.put("sort", sorts);
        SearchOptions searchOptions = new SearchOptions(parameters, "workspace1");

        QueryResultsIterableSearchResults results = elementSearchRunner.run(searchOptions, user, authorizations);
        assertEquals(2, size(results.getElements()));
        Iterator<? extends Element> elements = results.getElements().iterator();

        Element first = elements.next();
        assertEquals("B", first.getProperty("name").getValue());
        Element second = elements.next();
        assertEquals("A", second.getProperty("name").getValue());
    }
}