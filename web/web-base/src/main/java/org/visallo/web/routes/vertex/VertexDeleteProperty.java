package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.security.ACLProvider;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

public class VertexDeleteProperty implements ParameterizedHandler {
    private final Graph graph;
    private final WorkspaceHelper workspaceHelper;
    private final OntologyRepository ontologyRepository;
    private final ACLProvider aclProvider;
    private final boolean autoPublishComments;

    @Inject
    public VertexDeleteProperty(
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
            @Required(name = "graphVertexId") String graphVertexId,
            @Required(name = "propertyKey") String propertyKey,
            @Required(name = "propertyName") String propertyName,
            @ActiveWorkspaceId String workspaceId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(propertyName);

        Vertex graphVertex = graph.getVertex(graphVertexId, authorizations);
        if (!aclProvider.canDeleteProperty(graphVertex, propertyKey, propertyName, user)) {
            throw new VisalloAccessDeniedException(propertyName + " is not deleteable", user, graphVertexId);
        }

        boolean isComment = VisalloProperties.COMMENT.getPropertyName().equals(propertyName);
        if (isComment && autoPublishComments) {
            workspaceId = null;
        }

        workspaceHelper.deleteProperties(graphVertex, propertyKey, propertyName, ontologyProperty, workspaceId,
                authorizations, user);

        return VisalloResponse.SUCCESS;
    }
}
