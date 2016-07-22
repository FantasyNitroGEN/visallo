package org.visallo.web.routes.vertex;

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
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.AuthorizationsParameterProviderFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;

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
            @ActiveWorkspaceId(required = false) String workspaceId,
            User user
    ) throws Exception {
        HashSet<String> vertexStringIds = new HashSet<>(Arrays.asList(vertexIdsParam));
        GetAuthorizationsResult getAuthorizationsResult = getAuthorizations(request, fallbackToPublic, user);

        Iterable<Vertex> graphVertices = graph.getVertices(
                vertexStringIds,
                ClientApiConverter.SEARCH_FETCH_HINTS,
                getAuthorizationsResult.authorizations
        );
        ClientApiVertexMultipleResponse result = new ClientApiVertexMultipleResponse();
        result.setRequiredFallback(getAuthorizationsResult.requiredFallback);
        for (Vertex v : graphVertices) {
            result.getVertices().add(ClientApiConverter.toClientApiVertex(
                    v,
                    workspaceId,
                    getAuthorizationsResult.authorizations
            ));
        }
        return result;
    }

    protected GetAuthorizationsResult getAuthorizations(
            HttpServletRequest request,
            boolean fallbackToPublic,
            User user
    ) {
        GetAuthorizationsResult result = new GetAuthorizationsResult();
        result.requiredFallback = false;
        try {
            result.authorizations = AuthorizationsParameterProviderFactory.getAuthorizations(
                    request,
                    userRepository,
                    authorizationRepository,
                    workspaceRepository
            );
        } catch (VisalloAccessDeniedException ex) {
            if (fallbackToPublic) {
                result.authorizations = authorizationRepository.getGraphAuthorizations(user);
                result.requiredFallback = true;
            } else {
                throw ex;
            }
        }
        return result;
    }

    protected static class GetAuthorizationsResult {
        public Authorizations authorizations;
        public boolean requiredFallback;
    }
}
