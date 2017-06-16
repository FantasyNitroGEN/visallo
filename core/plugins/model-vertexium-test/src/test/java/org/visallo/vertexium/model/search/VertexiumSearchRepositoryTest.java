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
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.search.SearchProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloInMemoryTestBase;
import org.visallo.web.clientapi.model.ClientApiSearch;
import org.visallo.web.clientapi.model.ClientApiSearchListResponse;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.WorkspaceAccess;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.vertexium.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class VertexiumSearchRepositoryTest extends VisalloInMemoryTestBase {
    private static final Visibility VISIBILITY = Visibility.EMPTY;
    private static final String USER1_USERNAME = "USER123";
    private static final String USER2_USERNAME = "USER456";
    private static final String WORKSPACE_ID = "WS123";
    private static final String ID = "123";
    private static final String NAME = "search1";
    private static final String ID2 = "456";
    private static final String NAME2 = "search2";
    private static final String URL = "/vertex/search";
    private static final JSONObject PARAMETERS = new JSONObject(ImmutableMap.of("key1", "value1"));


    private VertexiumSearchRepository searchRepository;
    private Authorizations authorizations;

    private User user1;
    private User user2;

    @Mock
    private Injector injector;

    @Before
    public void before() {
        super.before();

        InjectHelper.setInjector(injector);

        authorizations = getGraph().createAuthorizations(
                VertexiumSearchRepository.VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );
        searchRepository = new VertexiumSearchRepository(
                getGraph(),
                getGraphRepository(),
                getUserRepository(),
                getConfiguration(),
                getGraphAuthorizationRepository(),
                getAuthorizationRepository(),
                getPrivilegeRepository(),
                getWorkspaceRepository()
        );

        user1 = getUserRepository().findOrAddUser(USER1_USERNAME, "User 1", "user1@visallo.com", "password");
        user2 = getUserRepository().findOrAddUser(USER2_USERNAME, "User 2", "user2@visallo.com", "password");
    }

    @Test
    public void testSaveSearch() {
        String foundId = searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
        assertEquals(ID, foundId);

        Vertex userVertex = getGraph().getVertex(user1.getUserId(), authorizations);
        List<Edge> hasSavedSearchEdges = toList(userVertex.getEdges(
                Direction.OUT,
                SearchProperties.HAS_SAVED_SEARCH,
                authorizations
        ));
        assertEquals(1, hasSavedSearchEdges.size());
        Vertex savedSearchVertex = hasSavedSearchEdges.get(0).getOtherVertex(user1.getUserId(), authorizations);
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
        setPrivileges(user1, Collections.singleton(Privilege.SEARCH_SAVE_GLOBAL));

        String foundId = searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user1);
        assertEquals(ID, foundId);

        Vertex savedSearchVertex = getGraph().getVertex(ID, authorizations);
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
        setPrivileges(user1, Collections.emptySet());

        try {
            searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
            fail("Expected VisalloAccessDeniedException");
        } catch (VisalloAccessDeniedException ex) {
            // expected
        }

        // User with the privilege can re-save as a non-global, private saved search.
        setPrivileges(user1, Collections.singleton(Privilege.SEARCH_SAVE_GLOBAL));

        foundId = searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
        assertEquals(ID, foundId);
    }

    @Test
    public void testSaveSearchAlternatingPrivateGlobalPrivate() {
        setPrivileges(user1, Collections.singleton(Privilege.SEARCH_SAVE_GLOBAL));

        searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
        assertSavedSearchState(false);

        searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user1);
        assertSavedSearchState(true);

        searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
        assertSavedSearchState(false);
    }

    @Test
    public void testSaveSearchAlternatingGlobalPrivateGlobal() {
        setPrivileges(user1, Collections.singleton(Privilege.SEARCH_SAVE_GLOBAL));

        searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user1);
        assertSavedSearchState(true);

        searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
        assertSavedSearchState(false);

        searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user1);
        assertSavedSearchState(true);
    }

    @Test
    public void testGetSavedSearches() {
        VertexBuilder vb = getGraph().prepareVertex(ID, VISIBILITY);
        VisalloProperties.CONCEPT_TYPE.setProperty(vb, SearchProperties.CONCEPT_TYPE_SAVED_SEARCH, VISIBILITY);
        SearchProperties.NAME.setProperty(vb, NAME, VISIBILITY);
        SearchProperties.URL.setProperty(vb, URL, VISIBILITY);
        SearchProperties.PARAMETERS.setProperty(vb, PARAMETERS, VISIBILITY);
        vb.save(authorizations);
        getGraph().addEdge(user1.getUserId(), ID, SearchProperties.HAS_SAVED_SEARCH, VISIBILITY, authorizations);
        getGraph().flush();

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
        VertexBuilder vb = getGraph().prepareVertex(ID, VISIBILITY);
        VisalloProperties.CONCEPT_TYPE.setProperty(vb, SearchProperties.CONCEPT_TYPE_SAVED_SEARCH, VISIBILITY);
        SearchProperties.NAME.setProperty(vb, NAME, VISIBILITY);
        SearchProperties.URL.setProperty(vb, URL, VISIBILITY);
        SearchProperties.PARAMETERS.setProperty(vb, PARAMETERS, VISIBILITY);
        vb.save(authorizations);
        getGraph().addEdge(user1.getUserId(), ID, SearchProperties.HAS_SAVED_SEARCH, VISIBILITY, authorizations);
        getGraph().flush();

        ClientApiSearch search = searchRepository.getSavedSearch(ID, user1);

        assertSearch(search);
    }

    @Test
    public void testGetSavedSearchOnWorkspaceShouldReturnIfGlobal() {
        Workspace workspace = setupForSearchOnWorkspaceTests(true);

        assertSearch(searchRepository.getSavedSearchOnWorkspace(ID, user1, WORKSPACE_ID));

        assertSearch(searchRepository.getSavedSearchOnWorkspace(ID, user2, WORKSPACE_ID));

        getWorkspaceRepository().updateUserOnWorkspace(workspace, user2.getUserId(), WorkspaceAccess.NONE, user1);
        assertSearch(searchRepository.getSavedSearchOnWorkspace(ID, user2, WORKSPACE_ID));
    }

    @Test
    public void testGetSavedSearchOnWorkspaceIfPrivate() {
        Workspace workspace = setupForSearchOnWorkspaceTests(false);

        assertSearch(searchRepository.getSavedSearchOnWorkspace(ID, user1, WORKSPACE_ID));

        assertSearch(searchRepository.getSavedSearchOnWorkspace(ID, user2, WORKSPACE_ID));

        getWorkspaceRepository().updateUserOnWorkspace(workspace, user2.getUserId(), WorkspaceAccess.NONE, user1);
        assertEquals(null, searchRepository.getSavedSearchOnWorkspace(ID, user2, WORKSPACE_ID));

        searchRepository.saveSearch(ID2, NAME2, URL, PARAMETERS, user2);
        assertEquals(null, searchRepository.getSavedSearchOnWorkspace(ID2, user1, WORKSPACE_ID));
    }

    private Workspace setupForSearchOnWorkspaceTests(boolean expectGlobal) {
        setPrivileges(user1, Collections.singleton(Privilege.SEARCH_SAVE_GLOBAL));
        Workspace workspace = getWorkspaceRepository().add(WORKSPACE_ID, "Search Workspace", user1);
        getWorkspaceRepository().updateUserOnWorkspace(workspace, user2.getUserId(), WorkspaceAccess.READ, user1);

        if (expectGlobal) {
            searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user1);
        } else {
            searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
        }
        return workspace;
    }

    @Test
    public void testDeleteSearch() {
        String foundId = searchRepository.saveSearch(ID, NAME, URL, PARAMETERS, user1);
        assertEquals(ID, foundId);

        searchRepository.deleteSearch(ID, user1);

        assertEquals(null, getGraph().getVertex(ID, authorizations));
    }

    @Test
    public void testDeleteGlobalSearch() {
        setPrivileges(user1, Collections.singleton(Privilege.SEARCH_SAVE_GLOBAL));
        String foundId = searchRepository.saveGlobalSearch(ID, NAME, URL, PARAMETERS, user1);
        assertEquals(ID, foundId);

        setPrivileges(user1, Collections.emptySet());

        try {
            searchRepository.deleteSearch(ID, user1);
            fail("Expected VisalloAccessDeniedException");
        } catch (VisalloAccessDeniedException ex) {
            // expected
        }

        setPrivileges(user1, Collections.singleton(Privilege.SEARCH_SAVE_GLOBAL));

        searchRepository.deleteSearch(ID, user1);

        assertEquals(null, getGraph().getVertex(ID, authorizations));
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
