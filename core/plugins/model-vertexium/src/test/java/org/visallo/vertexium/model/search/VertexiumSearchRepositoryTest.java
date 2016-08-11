package org.visallo.vertexium.model.search;

import com.google.common.collect.ImmutableMap;
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
import org.visallo.core.exception.VisalloAccessDeniedException;
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
import org.visallo.web.clientapi.model.Privilege;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.vertexium.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class VertexiumSearchRepositoryTest {
    private static final Visibility VISIBILITY = Visibility.EMPTY;
    private static final String USER_ID = "USER123";
    private static final String ID = "123";
    private static final String NAME = "search1";
    private static final String URL = "/vertex/search";
    private static final JSONObject PARAMETERS = new JSONObject(ImmutableMap.of("key1", "value1"));

    private VertexiumSearchRepository searchRepository;
    private InMemoryGraph graph;
    private Authorizations authorizations;

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

        when(user.getUserId()).thenReturn(USER_ID);
        graph.addVertex(USER_ID, VISIBILITY, authorizations);
        graph.flush();

        when(userRepository.getSystemUser()).thenReturn(systemUser);
        when(authorizationRepository.getGraphAuthorizations(
                eq(systemUser),
                eq(VertexiumSearchRepository.VISIBILITY_STRING)
        )).thenReturn(authorizations);

        when(authorizationRepository.getGraphAuthorizations(
                any(User.class),
                eq(VertexiumSearchRepository.VISIBILITY_STRING),
                eq(UserRepository.VISIBILITY_STRING)
        )).thenReturn(authorizations);
    }

    @Test
    public void testSaveSearch() {
        String foundId = searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user);
        assertEquals(ID, foundId);

        Vertex userVertex = graph.getVertex(USER_ID, authorizations);
        List<Edge> hasSavedSearchEdges = toList(userVertex.getEdges(
                Direction.OUT,
                SearchProperties.HAS_SAVED_SEARCH,
                authorizations
        ));
        assertEquals(1, hasSavedSearchEdges.size());
        Vertex savedSearchVertex = hasSavedSearchEdges.get(0).getOtherVertex(USER_ID, authorizations);
        assertEquals(
                SearchProperties.CONCEPT_TYPE_SAVED_SEARCH,
                VisalloProperties.CONCEPT_TYPE.getPropertyValue(savedSearchVertex, null)
        );
        assertEquals(NAME, SearchProperties.NAME.getPropertyValue(savedSearchVertex, null));
        assertEquals(URL, SearchProperties.URL.getPropertyValue(savedSearchVertex, null));
        assertEquals(
                PARAMETERS.toString(),
                SearchProperties.PARAMETERS.getPropertyValueRequired(savedSearchVertex).toString()
        );
    }

    @Test
    public void testSaveGlobalSearch() {
        // User without the SEARCH_SAVE_GLOBAL privilege can't save as global.

        try {
            searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user);
            fail("Expected VisalloAccessDeniedException");
        } catch (VisalloAccessDeniedException ex) {
            // expected
        }

        // User with the SEARCH_SAVE_GLOBAL privilege can save as global.

        when(privilegeRepository.hasPrivilege(user, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(true);

        String foundId = searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user);
        assertEquals(ID, foundId);

        Vertex savedSearchVertex = graph.getVertex(ID, authorizations);
        List<Edge> hasSavedSearchEdges = toList(savedSearchVertex.getEdges(
                Direction.IN,
                SearchProperties.HAS_SAVED_SEARCH,
                authorizations
        ));
        assertEquals(1, hasSavedSearchEdges.size());
        assertEquals(
                SearchProperties.CONCEPT_TYPE_SAVED_SEARCH,
                VisalloProperties.CONCEPT_TYPE.getPropertyValue(savedSearchVertex, null)
        );
        assertEquals(NAME, SearchProperties.NAME.getPropertyValue(savedSearchVertex, null));
        assertEquals(URL, SearchProperties.URL.getPropertyValue(savedSearchVertex, null));
        assertEquals(
                PARAMETERS.toString(),
                SearchProperties.PARAMETERS.getPropertyValueRequired(savedSearchVertex).toString()
        );

        // User without the privilege can't re-save as a non-global, private saved search.

        when(privilegeRepository.hasPrivilege(user, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(false);

        try {
            searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user);
            fail("Expected VisalloAccessDeniedException");
        } catch (VisalloAccessDeniedException ex) {
            // expected
        }

        // User with the privilege can re-save as a non-global, private saved search.

        when(privilegeRepository.hasPrivilege(user, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(true);

        foundId = searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user);
        assertEquals(ID, foundId);
    }

    @Test
    public void testSaveSearchAlternatingPrivateGlobalPrivate() {
        when(privilegeRepository.hasPrivilege(user, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(true);

        searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user);
        assertSavedSearchState(false);

        searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user);
        assertSavedSearchState(true);

        searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user);
        assertSavedSearchState(false);
    }

    @Test
    public void testSaveSearchAlternatingGlobalPrivateGlobal() {
        when(privilegeRepository.hasPrivilege(user, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(true);

        searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user);
        assertSavedSearchState(true);

        searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user);
        assertSavedSearchState(false);

        searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user);
        assertSavedSearchState(true);
    }

    @Test
    public void testGetSavedSearches() {
        VertexBuilder vb = graph.prepareVertex(ID, VISIBILITY);
        VisalloProperties.CONCEPT_TYPE.setProperty(vb, SearchProperties.CONCEPT_TYPE_SAVED_SEARCH, VISIBILITY);
        SearchProperties.NAME.setProperty(vb, NAME, VISIBILITY);
        SearchProperties.URL.setProperty(vb, URL, VISIBILITY);
        SearchProperties.PARAMETERS.setProperty(vb, PARAMETERS, VISIBILITY);
        vb.save(authorizations);
        graph.addEdge(USER_ID, ID, SearchProperties.HAS_SAVED_SEARCH, VISIBILITY, authorizations);
        graph.flush();

        ClientApiSearchListResponse response = searchRepository.getSavedSearches(user);

        assertEquals(1, response.searches.size());
        for (ClientApiSearch search : response.searches) {
            if (search.id.equals(ID)) {
                assertEquals(ID, search.id);
                assertEquals(NAME, search.name);
                assertEquals(URL, search.url);
                assertEquals(PARAMETERS.keySet().size(), search.parameters.size());
                assertEquals(PARAMETERS.getString("key1"), search.parameters.get("key1"));
            } else {
                throw new VisalloException("Invalid search item");
            }
        }
    }

    @Test
    public void testGetSavedSearch() {
        VertexBuilder vb = graph.prepareVertex(ID, VISIBILITY);
        VisalloProperties.CONCEPT_TYPE.setProperty(vb, SearchProperties.CONCEPT_TYPE_SAVED_SEARCH, VISIBILITY);
        SearchProperties.NAME.setProperty(vb, NAME, VISIBILITY);
        SearchProperties.URL.setProperty(vb, URL, VISIBILITY);
        SearchProperties.PARAMETERS.setProperty(vb, PARAMETERS, VISIBILITY);
        vb.save(authorizations);
        graph.addEdge(USER_ID, ID, SearchProperties.HAS_SAVED_SEARCH, VISIBILITY, authorizations);
        graph.flush();

        ClientApiSearch search = searchRepository.getSavedSearch(ID, user);

        assertEquals(ID, search.id);
        assertEquals(NAME, search.name);
        assertEquals(URL, search.url);
        assertEquals(PARAMETERS.keySet().size(), search.parameters.size());
        assertEquals(PARAMETERS.getString("key1"), search.parameters.get("key1"));
    }

    @Test
    public void testDeleteSearch() {
        String foundId = searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user);
        assertEquals(ID, foundId);

        searchRepository.deleteSearch(ID, user);

        assertEquals(null, graph.getVertex(ID, authorizations));
    }

    @Test
    public void testDeleteGlobalSearch() {
        when(privilegeRepository.hasPrivilege(user, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(true);
        String foundId = searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user);
        assertEquals(ID, foundId);
        when(privilegeRepository.hasPrivilege(user, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(false);

        try {
            searchRepository.deleteSearch(ID, user);
            fail("Expected VisalloAccessDeniedException");
        } catch (VisalloAccessDeniedException ex) {
            // expected
        }

        when(privilegeRepository.hasPrivilege(user, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(true);

        searchRepository.deleteSearch(ID, user);

        assertEquals(null, graph.getVertex(ID, authorizations));
    }

    private void assertSavedSearchState(boolean expectGlobal) {
        ClientApiSearchListResponse response = searchRepository.getSavedSearches(user);
        assertEquals(1, response.searches.size());
        assertEquals(searchRepository.isSearchGlobal(ID, authorizations), expectGlobal);
        assertEquals(searchRepository.isSearchPrivateToUser(ID, user, authorizations), !expectGlobal);
    }
}
