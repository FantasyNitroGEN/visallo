package org.visallo.vertexium.model.search;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.json.JSONObject;
import org.vertexium.*;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.search.SearchProperties;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.GraphAuthorizationRepository;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiSearch;
import org.visallo.web.clientapi.model.ClientApiSearchListResponse;
import org.visallo.web.clientapi.model.Privilege;

import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.visallo.core.util.StreamUtil.stream;

public class VertexiumSearchRepository extends SearchRepository {
    public static final String VISIBILITY_STRING = "search";
    public static final VisalloVisibility VISIBILITY = new VisalloVisibility(VISIBILITY_STRING);
    private static final String GLOBAL_SAVED_SEARCHES_ROOT_VERTEX_ID = "__visallo_globalSavedSearchesRoot";
    private final Graph graph;
    private final UserRepository userRepository;
    private final AuthorizationRepository authorizationRepository;
    private final PrivilegeRepository privilegeRepository;

    @Inject
    public VertexiumSearchRepository(
            Graph graph,
            UserRepository userRepository,
            Configuration configuration,
            GraphAuthorizationRepository graphAuthorizationRepository,
            AuthorizationRepository authorizationRepository,
            PrivilegeRepository privilegeRepository
    ) {
        super(configuration);
        this.graph = graph;
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
        this.privilegeRepository = privilegeRepository;
        graphAuthorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
    }

    @Override
    public String saveSearch(
            String id,
            String name,
            String url,
            JSONObject searchParameters,
            User user
    ) {
        checkNotNull(user, "User is required");
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );

        Vertex searchVertex = saveSearchVertex(id, name, url, searchParameters, authorizations);

        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        checkNotNull(userVertex, "Could not find user vertex with id " + user.getUserId());
        String edgeId = userVertex.getId() + "_" + SearchProperties.HAS_SAVED_SEARCH + "_" + searchVertex.getId();
        graph.addEdge(
                edgeId,
                userVertex,
                searchVertex,
                SearchProperties.HAS_SAVED_SEARCH,
                VISIBILITY.getVisibility(),
                authorizations
        );

        graph.flush();

        return searchVertex.getId();
    }

    @Override
    public String saveGlobalSearch(
            String id,
            String name,
            String url,
            JSONObject searchParameters,
            User user
    ) {
        if (!privilegeRepository.hasPrivilege(user, Privilege.ADMIN)) {
            throw new VisalloAccessDeniedException("User does not have access to save global searches", user, id);
        }

        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                userRepository.getSystemUser(),
                VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );

        Vertex searchVertex = saveSearchVertex(id, name, url, searchParameters, authorizations);

        String edgeId = GLOBAL_SAVED_SEARCHES_ROOT_VERTEX_ID + "_" + SearchProperties.HAS_SAVED_SEARCH + "_" + searchVertex.getId();
        graph.addEdge(
                edgeId,
                getGlobalSavedSearchesRootVertex(),
                searchVertex,
                SearchProperties.HAS_SAVED_SEARCH,
                VISIBILITY.getVisibility(),
                authorizations
        );

        graph.flush();

        return searchVertex.getId();
    }

    private Vertex saveSearchVertex(
            String id,
            String name,
            String url,
            JSONObject searchParameters,
            Authorizations authorizations
    ) {
        VertexBuilder searchVertexBuilder = graph.prepareVertex(id, VISIBILITY.getVisibility());
        VisalloProperties.CONCEPT_TYPE.setProperty(
                searchVertexBuilder,
                SearchProperties.CONCEPT_TYPE_SAVED_SEARCH,
                VISIBILITY.getVisibility()
        );
        SearchProperties.NAME.setProperty(searchVertexBuilder, name != null ? name : "", VISIBILITY.getVisibility());
        SearchProperties.URL.setProperty(searchVertexBuilder, url, VISIBILITY.getVisibility());
        SearchProperties.PARAMETERS.setProperty(searchVertexBuilder, searchParameters, VISIBILITY.getVisibility());
        return searchVertexBuilder.save(authorizations);
    }

    @Override
    public ClientApiSearchListResponse getSavedSearches(User user) {
        checkNotNull(user, "User is required");
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );

        ClientApiSearchListResponse result = new ClientApiSearchListResponse();
        Iterables.addAll(result.searches, getGlobalSavedSearches(authorizations));
        Iterables.addAll(result.searches, getUserSavedSearches(user, authorizations));
        return result;
    }

    private Iterable<ClientApiSearch> getUserSavedSearches(User user, Authorizations authorizations) {
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        checkNotNull(userVertex, "Could not find user vertex with id " + user.getUserId());
        Iterable<Vertex> userSearchVertices = userVertex.getVertices(
                Direction.OUT,
                SearchProperties.HAS_SAVED_SEARCH,
                authorizations
        );
        return stream(userSearchVertices)
                .map(searchVertex -> toClientApiSearch(searchVertex, ClientApiSearch.Scope.User))
                .collect(Collectors.toList());
    }

    private Iterable<ClientApiSearch> getGlobalSavedSearches(Authorizations authorizations) {
        Vertex globalSavedSearchesRootVertex = getGlobalSavedSearchesRootVertex();
        Iterable<Vertex> globalSearchVertices = globalSavedSearchesRootVertex.getVertices(
                Direction.OUT,
                SearchProperties.HAS_SAVED_SEARCH,
                authorizations
        );
        return stream(globalSearchVertices)
                .map(searchVertex -> toClientApiSearch(searchVertex, ClientApiSearch.Scope.Global))
                .collect(Collectors.toList());
    }

    private Vertex getGlobalSavedSearchesRootVertex() {
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                userRepository.getSystemUser(),
                VISIBILITY_STRING
        );
        Vertex globalSavedSearchesRootVertex = graph.getVertex(GLOBAL_SAVED_SEARCHES_ROOT_VERTEX_ID, authorizations);
        if (globalSavedSearchesRootVertex == null) {
            globalSavedSearchesRootVertex = graph.prepareVertex(
                    GLOBAL_SAVED_SEARCHES_ROOT_VERTEX_ID,
                    new Visibility(VISIBILITY_STRING)
            )
                    .save(authorizations);
            graph.flush();
        }
        return globalSavedSearchesRootVertex;
    }

    private static ClientApiSearch toClientApiSearch(Vertex searchVertex) {
        return toClientApiSearch(searchVertex, null);
    }

    public static ClientApiSearch toClientApiSearch(Vertex searchVertex, ClientApiSearch.Scope scope) {
        ClientApiSearch result = new ClientApiSearch();
        result.id = searchVertex.getId();
        result.name = SearchProperties.NAME.getPropertyValue(searchVertex);
        result.url = SearchProperties.URL.getPropertyValue(searchVertex);
        result.scope = scope;
        result.parameters = ClientApiConverter.toClientApiValue(SearchProperties.PARAMETERS.getPropertyValue(
                searchVertex));
        return result;
    }

    @Override
    public ClientApiSearch getSavedSearch(String id, User user) {
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );
        Vertex searchVertex = graph.getVertex(id, authorizations);
        if (searchVertex == null) {
            return null;
        }
        return toClientApiSearch(searchVertex);
    }

    @Override
    public void deleteSearch(final String savedSearchId, User user) {
        checkNotNull(user, "User is required");
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                user,
                VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING
        );
        Vertex searchVertex = graph.getVertex(savedSearchId, authorizations);
        checkNotNull(searchVertex, "Could not find search with id " + savedSearchId);

        if (!doesUserHaveAccess(savedSearchId, user, authorizations)) {
            throw new VertexiumException("User " + user.getUserId() + " does not have access to " + savedSearchId);
        }

        graph.deleteVertex(searchVertex, authorizations);
        graph.flush();
    }

    private boolean doesUserHaveAccess(String savedSearchId, User user, Authorizations authorizations) {
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        checkNotNull(userVertex, "Could not find user vertex with id " + user.getUserId());
        Iterable<String> vertexIds = userVertex.getVertexIds(
                Direction.OUT,
                SearchProperties.HAS_SAVED_SEARCH,
                authorizations
        );
        if (stream(vertexIds).anyMatch(vertexId -> vertexId.equals(savedSearchId))) {
            return true;
        }

        if (privilegeRepository.hasPrivilege(user, Privilege.ADMIN)) {
            vertexIds = getGlobalSavedSearchesRootVertex().getVertexIds(
                    Direction.OUT,
                    SearchProperties.HAS_SAVED_SEARCH,
                    authorizations
            );
            if (stream(vertexIds).anyMatch(vertexId -> vertexId.equals(savedSearchId))) {
                return true;
            }
        }

        return false;
    }
}
