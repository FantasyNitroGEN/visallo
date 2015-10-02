package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.*;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BadRequestException;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;
import org.visallo.web.clientapi.model.ClientApiVertexPropertyDetails;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.ResourceBundle;

public class VertexPropertyDetails implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexPropertyDetails.class);
    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexPropertyDetails(
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            TermMentionRepository termMentionRepository
    ) {
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.termMentionRepository = termMentionRepository;
    }

    @Handle
    public ClientApiVertexPropertyDetails handle(
            @Required(name = "vertexId") String vertexId,
            @Optional(name = "propertyKey") String propertyKey,
            @Required(name = "propertyName") String propertyName,
            @Required(name = "visibilitySource") String visibilitySource,
            @ActiveWorkspaceId String workspaceId,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations,
            VisalloResponse response
    ) throws Exception {
        Visibility visibility = visibilityTranslator.toVisibility(visibilitySource).getVisibility();
        if (!graph.isVisibilityValid(visibility, authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            throw new BadRequestException("visibilitySource", resourceBundle.getString("visibility.invalid"));
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
        return result;
    }
}
