package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.vertexium.Authorizations;
import org.vertexium.Element;
import org.vertexium.Vertex;
import org.visallo.core.model.search.SearchOptions;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.model.search.VertexFindRelatedSearchResults;
import org.visallo.core.model.search.VertexFindRelatedSearchRunner;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiElementFindRelatedResponse;
import org.visallo.web.clientapi.model.ClientApiVertex;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.routes.search.WebSearchOptionsFactory;

import javax.servlet.http.HttpServletRequest;

public class VertexFindRelated implements ParameterizedHandler {
    private final VertexFindRelatedSearchRunner searchRunner;

    @Inject
    public VertexFindRelated(SearchRepository searchRepository) {
        this.searchRunner =
                (VertexFindRelatedSearchRunner) searchRepository.findSearchRunnerByUri(VertexFindRelatedSearchRunner.URI);
    }

    @Handle
    public ClientApiElementFindRelatedResponse handle(
            HttpServletRequest request,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        SearchOptions searchOptions = WebSearchOptionsFactory.create(request, workspaceId);
        return getVertices(searchOptions, user, authorizations);
    }

    /**
     * This is overridable so web plugins can modify the resulting set of vertices.
     */
    protected ClientApiElementFindRelatedResponse getVertices(
            SearchOptions searchOptions,
            User user,
            Authorizations authorizations
    ) {
        VertexFindRelatedSearchResults results = this.searchRunner.run(searchOptions, user, authorizations);
        ClientApiElementFindRelatedResponse response = new ClientApiElementFindRelatedResponse();
        for (Element element : results.getElements()) {
            Vertex vertex = (Vertex) element;
            ClientApiVertex clientApiVertex = ClientApiConverter.toClientApiVertex(vertex, searchOptions.getWorkspaceId(), authorizations);
            response.getElements().add(clientApiVertex);
        }
        response.setCount(results.getCount());
        return response;
    }

}

