package org.visallo.vertexium.model.search;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.json.JSONObject;
import org.vertexium.*;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.search.SearchProperties;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiSearch;
import org.visallo.web.clientapi.model.ClientApiSearchListResponse;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public class VertexiumSearchRepository extends SearchRepository {
    public static final String VISIBILITY_STRING = "search";
    public static final VisalloVisibility VISIBILITY = new VisalloVisibility(VISIBILITY_STRING);
    private final Graph graph;
    private final UserRepository userRepository;

    @Inject
    public VertexiumSearchRepository(
            Graph graph,
            UserRepository userRepository,
            AuthorizationRepository authorizationRepository
    ) {
        this.graph = graph;
        this.userRepository = userRepository;
        authorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
    }

    @Override
    public String saveSearch(User user, String id, String name, String url, JSONObject searchParameters) {
        checkNotNull(user, "User is required");
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, UserRepository.VISIBILITY_STRING);
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        checkNotNull(userVertex, "Could not find user vertex with id " + user.getUserId());

        VertexBuilder searchVertexBuilder = graph.prepareVertex(id, VISIBILITY.getVisibility());
        VisalloProperties.CONCEPT_TYPE.setProperty(searchVertexBuilder, SearchProperties.CONCEPT_TYPE_SAVED_SEARCH, VISIBILITY.getVisibility());
        SearchProperties.NAME.setProperty(searchVertexBuilder, name != null ? name : "", VISIBILITY.getVisibility());
        SearchProperties.URL.setProperty(searchVertexBuilder, url, VISIBILITY.getVisibility());
        SearchProperties.PARAMETERS.setProperty(searchVertexBuilder, searchParameters, VISIBILITY.getVisibility());
        Vertex searchVertex = searchVertexBuilder.save(authorizations);

        graph.addEdge(userVertex, searchVertex, SearchProperties.HAS_SAVED_SEARCH, VISIBILITY.getVisibility(), authorizations);

        graph.flush();

        return searchVertex.getId();
    }

    @Override
    public ClientApiSearchListResponse getSavedSearches(User user) {
        checkNotNull(user, "User is required");
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, UserRepository.VISIBILITY_STRING);
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        checkNotNull(userVertex, "Could not find user vertex with id " + user.getUserId());

        Iterable<Vertex> searchVertices = userVertex.getVertices(Direction.OUT, SearchProperties.HAS_SAVED_SEARCH, authorizations);
        ClientApiSearchListResponse result = new ClientApiSearchListResponse();
        result.searches = Lists.newArrayList(Iterables.transform(searchVertices, new Function<Vertex, ClientApiSearch>() {
            @Nullable
            @Override
            public ClientApiSearch apply(Vertex searchVertex) {
                return toClientApiSearch(searchVertex);
            }
        }));
        return result;
    }

    private ClientApiSearch toClientApiSearch(Vertex searchVertex) {
        ClientApiSearch result = new ClientApiSearch();
        result.id = searchVertex.getId();
        result.name = SearchProperties.NAME.getPropertyValue(searchVertex);
        result.url = SearchProperties.URL.getPropertyValue(searchVertex);
        result.parameters = ClientApiConverter.toClientApiValue(SearchProperties.PARAMETERS.getPropertyValue(searchVertex));
        return result;
    }

    @Override
    public ClientApiSearch getSavedSearch(String id, User user) {
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, UserRepository.VISIBILITY_STRING);
        Vertex searchVertex = graph.getVertex(id, authorizations);
        if (searchVertex == null) {
            return null;
        }
        return toClientApiSearch(searchVertex);
    }

    @Override
    public void deleteSearch(final String id, User user) {
        checkNotNull(user, "User is required");
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, UserRepository.VISIBILITY_STRING);
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        checkNotNull(userVertex, "Could not find user vertex with id " + user.getUserId());
        Vertex searchVertex = graph.getVertex(id, authorizations);
        checkNotNull(userVertex, "Could not find search with id " + id);

        if (!Iterables.any(userVertex.getVertexIds(Direction.OUT, SearchProperties.HAS_SAVED_SEARCH, authorizations), new Predicate<String>() {
            @Override
            public boolean apply(String vertexId) {
                return vertexId.equals(id);
            }
        })) {
            throw new VertexiumException("User " + userVertex.getId() + " does not have access to " + id);
        }

        graph.deleteVertex(searchVertex, authorizations);
        graph.flush();
    }
}
