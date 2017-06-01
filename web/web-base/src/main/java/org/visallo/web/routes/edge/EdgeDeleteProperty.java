package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.security.ACLProvider;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import javax.servlet.http.HttpServletRequest;

public class EdgeDeleteProperty implements ParameterizedHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;
    private final WorkspaceHelper workspaceHelper;
    private final ACLProvider aclProvider;
    private final boolean autoPublishComments;

    @Inject
    public EdgeDeleteProperty(
            final Graph graph,
            final WorkspaceHelper workspaceHelper,
            final OntologyRepository ontologyRepository,
            final ACLProvider aclProvider,
            final Configuration configuration
    ) {
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
        this.ontologyRepository = ontologyRepository;
        this.aclProvider = aclProvider;
        this.autoPublishComments = configuration.getBoolean(Configuration.COMMENTS_AUTO_PUBLISH,
                Configuration.DEFAULT_COMMENTS_AUTO_PUBLISH);
    }

    @Handle
    public ClientApiSuccess handle(
            HttpServletRequest request,
            @Required(name = "edgeId") String edgeId,
            @Required(name = "propertyKey") String propertyKey,
            @Required(name = "propertyName") String propertyName,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        OntologyProperty ontologyProperty = ontologyRepository.getRequiredPropertyByIRI(propertyName);

        // TODO remove all properties from all edges? I don't think so
        Edge edge = graph.getEdge(edgeId, authorizations);

        aclProvider.checkCanDeleteProperty(edge, propertyKey, propertyName, user, workspaceId);

        boolean isComment = VisalloProperties.COMMENT.isSameName(propertyName);

        if (isComment && request.getPathInfo().equals("/edge/property")) {
            throw new VisalloException("Use /edge/comment to save comment properties");
        } else if (!isComment && request.getPathInfo().equals("/edge/comment")) {
            throw new VisalloException("Use /edge/property to save non-comment properties");
        }

        if (isComment && autoPublishComments) {
            workspaceId = null;
        }

        workspaceHelper.deleteProperties(edge, propertyKey, propertyName, ontologyProperty, workspaceId, authorizations,
                user);

        return VisalloResponse.SUCCESS;
    }
}
