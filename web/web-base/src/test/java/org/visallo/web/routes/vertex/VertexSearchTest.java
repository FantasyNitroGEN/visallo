package org.visallo.web.routes.vertex;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Authorizations;
import org.vertexium.ElementBuilder;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.visallo.core.model.directory.DirectoryRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.web.RouteTestBase;
import org.visallo.web.clientapi.model.ClientApiElementSearchResponse;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VertexSearchTest extends RouteTestBase {
    private VertexSearch vertexSearch;
    private Visibility visibility;
    private Authorizations authorizations;

    @Mock
    private DirectoryRepository directoryRepository;

    @Before
    public void setUp() throws IOException {
        super.setUp();

        visibility = new Visibility("");
        authorizations = graph.createAuthorizations("");

        vertexSearch = new VertexSearch(ontologyRepository, graph, configuration, directoryRepository);
    }

    @Test
    public void testSearchRelated() throws Exception {
        ElementBuilder<Vertex> v1Mutation = graph.prepareVertex("v1", visibility);
        ElementBuilder<Vertex> v2Mutation = graph.prepareVertex("v2", visibility);
        ElementBuilder<Vertex> v3Mutation = graph.prepareVertex("v3", visibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(v1Mutation, "testConcept", visibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(v2Mutation, "testConcept", visibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(v3Mutation, "testConcept", visibility);
        Vertex v1 = v1Mutation.save(authorizations);
        Vertex v2 = v2Mutation.save(authorizations);
        Vertex v3 = v3Mutation.save(authorizations);
        graph.addEdge("e1", v1, v2, "label1", visibility, authorizations);
        graph.addEdge("e2", v1, v3, "label1", visibility, authorizations);
        graph.flush();

        setArrayParameter("relatedToVertexIds[]", new String[]{"v1"});
        setParameter("filter", new JSONArray());

        when(userRepository.getAuthorizations(eq(user), eq(WORKSPACE_ID))).thenReturn(authorizations);

        ClientApiElementSearchResponse response = vertexSearch.handle(request, WORKSPACE_ID, user, authorizations);
        assertEquals(2, response.getElements().size());
        assertEquals(2, response.getItemCount());
    }
}