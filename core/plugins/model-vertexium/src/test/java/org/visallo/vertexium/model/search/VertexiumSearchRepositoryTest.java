package org.visallo.vertexium.model.search;

import com.google.inject.Injector;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.search.SearchProperties;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.GraphAuthorizationRepository;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiSearch;
import org.visallo.web.clientapi.model.ClientApiSearchListResponse;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.vertexium.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class VertexiumSearchRepositoryTest {
    private VertexiumSearchRepository searchRepository;
    private InMemoryGraph graph;
    private Authorizations authorizations;
    private String userId;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GraphAuthorizationRepository graphAuthorizationRepository;

    @Mock
    private Configuration configuration;

    @Mock
    private User user;

    @Mock
    private Injector injector;

    @Mock
    private User systemUser;

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Mock
    private PrivilegeRepository privilegeRepository;

    @Before
    public void setUp() {
        InjectHelper.setInjector(injector);

        graph = InMemoryGraph.create();
        authorizations = graph.createAuthorizations(
                VertexiumSearchRepository.VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );
        searchRepository = new VertexiumSearchRepository(
                graph,
                userRepository,
                configuration,
                graphAuthorizationRepository,
                authorizationRepository,
                privilegeRepository
        );

        userId = "USER123";
        when(user.getUserId()).thenReturn(userId);
        graph.addVertex(userId, new Visibility(""), authorizations);
        graph.flush();

        when(userRepository.getSystemUser()).thenReturn(systemUser);
        when(authorizationRepository.getGraphAuthorizations(
                eq(systemUser),
                eq(VertexiumSearchRepository.VISIBILITY_STRING)
        )).thenReturn(authorizations);

        when(authorizationRepository.getGraphAuthorizations(
                eq(user),
                eq(VertexiumSearchRepository.VISIBILITY_STRING),
                eq(UserRepository.VISIBILITY_STRING)
        )).thenReturn(authorizations);
    }

    @Test
    public void testSaveSearch() {
        String id = "123";
        String name = "search1";
        String url = "/vertex/search";
        JSONObject searchParameters = new JSONObject();
        searchParameters.put("key1", "value1");

        String foundId = searchRepository.saveSearch(id, name, url, searchParameters, user);
        assertEquals(id, foundId);

        Vertex userVertex = graph.getVertex(userId, authorizations);
        List<Edge> hasSavedSearchEdges = toList(userVertex.getEdges(
                Direction.OUT,
                SearchProperties.HAS_SAVED_SEARCH,
                authorizations
        ));
        assertEquals(1, hasSavedSearchEdges.size());
        Vertex savedSearchVertex = hasSavedSearchEdges.get(0).getOtherVertex(userId, authorizations);
        assertEquals(
                SearchProperties.CONCEPT_TYPE_SAVED_SEARCH,
                VisalloProperties.CONCEPT_TYPE.getPropertyValue(savedSearchVertex, null)
        );
        assertEquals(name, SearchProperties.NAME.getPropertyValue(savedSearchVertex, null));
        assertEquals(url, SearchProperties.URL.getPropertyValue(savedSearchVertex, null));
        assertEquals(
                searchParameters.toString(),
                SearchProperties.PARAMETERS.getPropertyValueRequired(savedSearchVertex).toString()
        );
    }

    @Test
    public void testGetSavedSearched() {
        String id = "SS123";
        String name = "saved search 123";
        String url = "/vertex/search";
        JSONObject parameters = new JSONObject();
        parameters.put("key1", "value1");
        VertexBuilder vb = graph.prepareVertex(id, new Visibility(""));
        VisalloProperties.CONCEPT_TYPE.setProperty(vb, SearchProperties.CONCEPT_TYPE_SAVED_SEARCH, new Visibility(""));
        SearchProperties.NAME.setProperty(vb, name, new Visibility(""));
        SearchProperties.URL.setProperty(vb, url, new Visibility(""));
        SearchProperties.PARAMETERS.setProperty(vb, parameters, new Visibility(""));
        vb.save(authorizations);

        graph.addEdge(userId, id, SearchProperties.HAS_SAVED_SEARCH, new Visibility(""), authorizations);

        graph.flush();

        ClientApiSearchListResponse response = searchRepository.getSavedSearches(user);
        assertEquals(1, response.searches.size());
        for (ClientApiSearch search : response.searches) {
            if (search.id.equals(id)) {
                assertEquals(id, search.id);
                assertEquals(name, search.name);
                assertEquals(url, search.url);
                assertEquals(parameters.keySet().size(), search.parameters.size());
                assertEquals(parameters.getString("key1"), search.parameters.get("key1"));
            } else {
                throw new VisalloException("Invalid search item");
            }
        }
    }

    @Test
    public void testGetSavedSearch() {
        String id = "SS123";
        String name = "saved search 123";
        String url = "/vertex/search";
        JSONObject parameters = new JSONObject();
        parameters.put("key1", "value1");
        VertexBuilder vb = graph.prepareVertex(id, new Visibility(""));
        VisalloProperties.CONCEPT_TYPE.setProperty(vb, SearchProperties.CONCEPT_TYPE_SAVED_SEARCH, new Visibility(""));
        SearchProperties.NAME.setProperty(vb, name, new Visibility(""));
        SearchProperties.URL.setProperty(vb, url, new Visibility(""));
        SearchProperties.PARAMETERS.setProperty(vb, parameters, new Visibility(""));
        vb.save(authorizations);

        graph.addEdge(userId, id, SearchProperties.HAS_SAVED_SEARCH, new Visibility(""), authorizations);

        graph.flush();

        ClientApiSearch search = searchRepository.getSavedSearch(id, user);
        assertEquals(id, search.id);
        assertEquals(name, search.name);
        assertEquals(url, search.url);
        assertEquals(parameters.keySet().size(), search.parameters.size());
        assertEquals(parameters.getString("key1"), search.parameters.get("key1"));
    }

    @Test
    public void testDeleteSearch() {
        String id = "SS123";
        String name = "saved search 123";
        String url = "/vertex/search";
        JSONObject parameters = new JSONObject();
        parameters.put("key1", "value1");
        VertexBuilder vb = graph.prepareVertex(id, new Visibility(""));
        VisalloProperties.CONCEPT_TYPE.setProperty(vb, SearchProperties.CONCEPT_TYPE_SAVED_SEARCH, new Visibility(""));
        SearchProperties.NAME.setProperty(vb, name, new Visibility(""));
        SearchProperties.URL.setProperty(vb, url, new Visibility(""));
        SearchProperties.PARAMETERS.setProperty(vb, parameters, new Visibility(""));
        vb.save(authorizations);

        graph.addEdge(userId, id, SearchProperties.HAS_SAVED_SEARCH, new Visibility(""), authorizations);

        graph.flush();

        assertNotEquals(null, graph.getVertex(id, authorizations));
        searchRepository.deleteSearch(id, user);
        assertEquals(null, graph.getVertex(id, authorizations));
    }
}