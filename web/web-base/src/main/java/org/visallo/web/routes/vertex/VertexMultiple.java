package org.visallo.web.routes.vertex;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiVertexMultipleResponse;
import org.visallo.web.parameterProviders.VisalloBaseParameterProvider;

import javax.servlet.http.HttpServletRequest;

public class VertexMultiple implements ParameterizedHandler {
    private final Graph graph;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public VertexMultiple(
            Graph graph,
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            AuthorizationRepository authorizationRepository
    ) {
        this.graph = graph;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.authorizationRepository = authorizationRepository;
    }

    @Handle
    public ClientApiVertexMultipleResponse handle(
            HttpServletRequest request,
            @Required(name = "vertexIds[]") String[] vertexIdsParam,
            @Optional(name = "fallbackToPublic", defaultValue = "false") boolean fallbackToPublic,
            User user
    ) throws Exception {
        ClientApiVertexMultipleResponse result = new ClientApiVertexMultipleResponse();

        String workspaceId = null;
        try {
            workspaceId = VisalloBaseParameterProvider.getActiveWorkspaceIdOrDefault(request, workspaceRepository, userRepository);
            result.setRequiredFallback(false);
        } catch (VisalloAccessDeniedException ex) {
            if (fallbackToPublic) {
                result.setRequiredFallback(true);
            } else {
                throw ex;
            }
        }

        Authorizations authorizations = workspaceId != null ?
                authorizationRepository.getGraphAuthorizations(user, workspaceId) :
                authorizationRepository.getGraphAuthorizations(user);

        Iterable<Vertex> graphVertices = graph.getVertices(
                Sets.newHashSet(vertexIdsParam),
                ClientApiConverter.SEARCH_FETCH_HINTS,
                authorizations
        );

        for (Vertex v : graphVertices) {
            result.getVertices().add(ClientApiConverter.toClientApiVertex(
                    v,
                    workspaceId,
                    authorizations
            ));
        }
        return result;
    }
}
