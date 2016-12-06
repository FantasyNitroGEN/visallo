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
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.search.SearchProperties;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.GraphAuthorizationRepository;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.DirectVisibilityTranslator;
import org.visallo.core.security.VisibilityTranslator;
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
    private static final String USER1_ID = "USER123";
    private static final String USER2_ID = "USER456";
    private static final String WORKSPACE_ID = "WS123";
    private static final String ID = "123";
    private static final String NAME = "search1";
    private static final String ID2 = "456";
    private static final String NAME2 = "search2";
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
    private User user1;

    @Mock
    private User user2;

    @Mock
    private Injector injector;

    @Mock
    private User systemUser;

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Mock
    private PrivilegeRepository privilegeRepository;

    @Mock
    private WorkQueueRepository workQueueRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Before
    public void setUp() {
        InjectHelper.setInjector(injector);

        graph = InMemoryGraph.create();
        authorizations = graph.createAuthorizations(
                VertexiumSearchRepository.VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );
        VisibilityTranslator visibilityTranslator = new DirectVisibilityTranslator();
        GraphRepository graphRepository = new GraphRepository(graph, visibilityTranslator, null, workQueueRepository);
        searchRepository = new VertexiumSearchRepository(
                graph,
                graphRepository,
                userRepository,
                configuration,
                graphAuthorizationRepository,
                authorizationRepository,
                privilegeRepository,
                workspaceRepository
        );

        when(user1.getUserId()).thenReturn(USER1_ID);
        when(user2.getUserId()).thenReturn(USER2_ID);
        graph.addVertex(USER1_ID, VISIBILITY, authorizations);
        graph.addVertex(USER2_ID, VISIBILITY, authorizations);
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
        String foundId = searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
        assertEquals(ID, foundId);

        Vertex userVertex = graph.getVertex(USER1_ID, authorizations);
        List<Edge> hasSavedSearchEdges = toList(userVertex.getEdges(
                Direction.OUT,
                SearchProperties.HAS_SAVED_SEARCH,
                authorizations
        ));
        assertEquals(1, hasSavedSearchEdges.size());
        Vertex savedSearchVertex = hasSavedSearchEdges.get(0).getOtherVertex(USER1_ID, authorizations);
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
            searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user1);
            fail("Expected VisalloAccessDeniedException");
        } catch (VisalloAccessDeniedException ex) {
            // expected
        }

        // User with the SEARCH_SAVE_GLOBAL privilege can save as global.

        when(privilegeRepository.hasPrivilege(user1, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(true);

        String foundId = searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user1);
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

        when(privilegeRepository.hasPrivilege(user1, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(false);

        try {
            searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
            fail("Expected VisalloAccessDeniedException");
        } catch (VisalloAccessDeniedException ex) {
            // expected
        }

        // User with the privilege can re-save as a non-global, private saved search.

        when(privilegeRepository.hasPrivilege(user1, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(true);

        foundId = searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
        assertEquals(ID, foundId);
    }

    @Test
    public void testSaveSearchAlternatingPrivateGlobalPrivate() {
        when(privilegeRepository.hasPrivilege(user1, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(true);

        searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
        assertSavedSearchState(false);

        searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user1);
        assertSavedSearchState(true);

        searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
        assertSavedSearchState(false);
    }

    @Test
    public void testSaveSearchAlternatingGlobalPrivateGlobal() {
        when(privilegeRepository.hasPrivilege(user1, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(true);

        searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user1);
        assertSavedSearchState(true);

        searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
        assertSavedSearchState(false);

        searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user1);
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
        graph.addEdge(USER1_ID, ID, SearchProperties.HAS_SAVED_SEARCH, VISIBILITY, authorizations);
        graph.flush();

        ClientApiSearchListResponse response = searchRepository.getSavedSearches(user1);

        assertEquals(1, response.searches.size());
        for (ClientApiSearch search : response.searches) {
            if (search.id.equals(ID)) {
                assertSearch(search);
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
        graph.addEdge(USER1_ID, ID, SearchProperties.HAS_SAVED_SEARCH, VISIBILITY, authorizations);
        graph.flush();

        ClientApiSearch search = searchRepository.getSavedSearch(ID, user1);

        assertSearch(search);
    }

    @Test
    public void testGetSavedSearchOnWorkspaceShouldReturnIfGlobal() {
        setupForSearchOnWorkspaceTests(true);

        assertSearch(searchRepository.getSavedSearchOnWorkspace(ID, user1, WORKSPACE_ID));

        assertSearch(searchRepository.getSavedSearchOnWorkspace(ID, user2, WORKSPACE_ID));

        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user2)).thenReturn(false);
        assertSearch(searchRepository.getSavedSearchOnWorkspace(ID, user2, WORKSPACE_ID));
    }

    @Test
    public void testGetSavedSearchOnWorkspaceIfPrivate() {
        setupForSearchOnWorkspaceTests(false);

        assertSearch(searchRepository.getSavedSearchOnWorkspace(ID, user1, WORKSPACE_ID));

        assertSearch(searchRepository.getSavedSearchOnWorkspace(ID, user2, WORKSPACE_ID));

        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user2)).thenReturn(false);
        assertEquals(null, searchRepository.getSavedSearchOnWorkspace(ID, user2, WORKSPACE_ID));

        searchRepository.saveSearch(ID2, NAME2, URL, PARAMETERS, user2);
        assertEquals(null, searchRepository.getSavedSearchOnWorkspace(ID2, user1, WORKSPACE_ID));
    }

    private void setupForSearchOnWorkspaceTests(boolean expectGlobal) {
        when(privilegeRepository.hasPrivilege(user1, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(true);
        when(workspaceRepository.getCreatorUserId(eq(WORKSPACE_ID), any(User.class))).thenReturn(USER1_ID);
        when(userRepository.findById(USER1_ID)).thenReturn(user1);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user1)).thenReturn(true);
        when(workspaceRepository.hasReadPermissions(WORKSPACE_ID, user2)).thenReturn(true);

        if (expectGlobal) {
            searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user1);
        } else {
            searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
        }
    }

    @Test
    public void testDeleteSearch() {
        String foundId = searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
        assertEquals(ID, foundId);

        searchRepository.deleteSearch(ID, user1);

        assertEquals(null, graph.getVertex(ID, authorizations));
    }

    @Test
    public void testDeleteGlobalSearch() {
        when(privilegeRepository.hasPrivilege(user1, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(true);
        String foundId = searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user1);
        assertEquals(ID, foundId);
        when(privilegeRepository.hasPrivilege(user1, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(false);

        try {
            searchRepository.deleteSearch(ID, user1);
            fail("Expected VisalloAccessDeniedException");
        } catch (VisalloAccessDeniedException ex) {
            // expected
        }

        when(privilegeRepository.hasPrivilege(user1, Privilege.SEARCH_SAVE_GLOBAL)).thenReturn(true);

        searchRepository.deleteSearch(ID, user1);

        assertEquals(null, graph.getVertex(ID, authorizations));
    }

    private void assertSearch(ClientApiSearch search) {
        assertEquals(ID, search.id);
        assertEquals(NAME, search.name);
        assertEquals(URL, search.url);
        assertEquals(PARAMETERS.keySet().size(), search.parameters.size());
        assertEquals(PARAMETERS.getString("key1"), search.parameters.get("key1"));
    }

    private void assertSavedSearchState(boolean expectGlobal) {
        ClientApiSearchListResponse response = searchRepository.getSavedSearches(user1);
        assertEquals(1, response.searches.size());
        assertEquals(searchRepository.isSearchGlobal(ID, authorizations), expectGlobal);
        assertEquals(searchRepository.isSearchPrivateToUser(ID, user1, authorizations), !expectGlobal);
    }
}
