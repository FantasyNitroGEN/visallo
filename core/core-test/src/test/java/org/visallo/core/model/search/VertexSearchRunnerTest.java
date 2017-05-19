package org.visallo.core.model.search;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.ElementBuilder;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.visallo.core.model.properties.VisalloProperties;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Iterables.size;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class VertexSearchRunnerTest extends SearchRunnerTestBase {
    private VertexSearchRunner vertexSearchRunner;

    @Before
    public void before() {
        super.before();

        vertexSearchRunner = new VertexSearchRunner(
                ontologyRepository,
                graph,
                configuration,
                directoryRepository
        );
    }

    @Test
    public void testSearchRelated() throws Exception {
        ElementBuilder<Vertex> v1Mutation = graph.prepareVertex("v1", visibility);
        ElementBuilder<Vertex> v2Mutation = graph.prepareVertex("v2", visibility);
        ElementBuilder<Vertex> v3Mutation = graph.prepareVertex("v3", visibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(v1Mutation, "testConcept", new Visibility(""));
        VisalloProperties.CONCEPT_TYPE.setProperty(v2Mutation, "testConcept", new Visibility(""));
        VisalloProperties.CONCEPT_TYPE.setProperty(v3Mutation, "testConcept", new Visibility(""));
        Vertex v1 = v1Mutation.save(authorizations);
        Vertex v2 = v2Mutation.save(authorizations);
        Vertex v3 = v3Mutation.save(authorizations);
        graph.addEdge("e1", v1, v2, "label1", visibility, authorizations);
        graph.addEdge("e2", v1, v3, "label1", visibility, authorizations);
        graph.flush();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("relatedToVertexIds[]", new String[]{"v1"});
        parameters.put("filter", new JSONArray());
        SearchOptions searchOptions = new SearchOptions(parameters, "workspace1");

        QueryResultsIterableSearchResults results = vertexSearchRunner.run(searchOptions, user, authorizations);
        assertEquals(2, size(results.getVertexiumObjects()));
    }

    @Test
    public void testSearch() throws Exception {
        graph.prepareVertex("v1", visibility)
                .addPropertyValue("k1", "name", "Joe", visibility)
                .save(authorizations);
        graph.prepareVertex("v2", visibility)
                .addPropertyValue("k1", "name", "Bob", visibility)
                .save(authorizations);
        graph.flush();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("q", "*");
        parameters.put("filter", new JSONArray());
        SearchOptions searchOptions = new SearchOptions(parameters, "workspace1");

        QueryResultsIterableSearchResults results = vertexSearchRunner.run(searchOptions, user, authorizations);
        assertEquals(2, size(results.getVertexiumObjects()));
    }
}