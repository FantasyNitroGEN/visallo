package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;
import org.visallo.web.clientapi.model.ClientApiVertexPropertyDetails;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.vertexium.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VertexPropertyDetails extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexPropertyDetails.class);
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexPropertyDetails(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration,
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            TermMentionRepository termMentionRepository
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.termMentionRepository = termMentionRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String vertexId = getRequiredParameter(request, "vertexId");
        String propertyName = getRequiredParameter(request, "propertyName");
        String visibilitySource = getRequiredParameter(request, "visibilitySource");
        String propertyKey = getOptionalParameter(request, "propertyKey");
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        Visibility visibility = new Visibility(visibilitySource);
        if (!graph.isVisibilityValid(visibility, authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", getString(request, "visibility.invalid"));
            chain.next(request, response);
            return;
        }

        Vertex vertex = this.graph.getVertex(vertexId, authorizations);
        if (vertex == null) {
            throw new VisalloResourceNotFoundException("Could not find vertex with id: " + vertexId, vertexId);
        }

        Property property = vertex.getProperty(propertyKey, propertyName, visibility);
        if (property == null) {
            VisibilityJson visibilityJson = new VisibilityJson();
            visibilityJson.setSource(visibilitySource);
            visibilityJson.addWorkspace(workspaceId);
            VisalloVisibility v2 = visibilityTranslator.toVisibility(visibilityJson);
            property = vertex.getProperty(propertyKey, propertyName, v2.getVisibility());
            if (property == null) {
                throw new VisalloResourceNotFoundException("Could not find property " + propertyKey + ":" + propertyName + ":" + visibility + " on vertex with id: " + vertexId, vertexId);
            }
        }

        ClientApiSourceInfo sourceInfo = termMentionRepository.getSourceInfoForVertexProperty(vertex.getId(), property, authorizations);

        ClientApiVertexPropertyDetails result = new ClientApiVertexPropertyDetails();
        result.sourceInfo = sourceInfo;

        respondWithClientApiObject(response, result);
    }
}
