package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.vertexium.Visibility;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BadRequestException;
import org.visallo.web.clientapi.model.ClientApiEdgePropertyDetails;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;

import java.util.ResourceBundle;

public class EdgePropertyDetails implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(EdgePropertyDetails.class);
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public EdgePropertyDetails(
            Graph graph,
            TermMentionRepository termMentionRepository,
            VisibilityTranslator visibilityTranslator
    ) {
        this.graph = graph;
        this.termMentionRepository = termMentionRepository;
        this.visibilityTranslator = visibilityTranslator;
    }

    @Handle
    public ClientApiEdgePropertyDetails handle(
            @Required(name = "edgeId") String edgeId,
            @Optional(name = "propertyKey") String propertyKey,
            @Required(name = "propertyName") String propertyName,
            @Required(name = "visibilitySource") String visibilitySource,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Visibility visibility = visibilityTranslator.toVisibility(visibilitySource).getVisibility();
        if (!graph.isVisibilityValid(visibility, authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            throw new BadRequestException("visibilitySource", resourceBundle.getString("visibility.invalid"));
        }

        Edge edge = this.graph.getEdge(edgeId, authorizations);
        if (edge == null) {
            throw new VisalloResourceNotFoundException("Could not find edge with id: " + edgeId, edgeId);
        }

        ClientApiSourceInfo sourceInfo = termMentionRepository.getSourceInfoForEdgeProperty(edge, propertyKey, propertyName, visibility, authorizations);

        ClientApiEdgePropertyDetails result = new ClientApiEdgePropertyDetails();
        result.sourceInfo = sourceInfo;
        return result;
    }
}
