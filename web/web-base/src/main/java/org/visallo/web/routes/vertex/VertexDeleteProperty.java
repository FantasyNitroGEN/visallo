package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.SandboxStatus;
import org.visallo.web.routes.workspace.WorkspaceHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import static org.vertexium.util.IterableUtils.toList;

public class VertexDeleteProperty extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexDeleteProperty.class);
    private final Graph graph;
    private final WorkspaceHelper workspaceHelper;
    private final OntologyRepository ontologyRepository;

    @Inject
    public VertexDeleteProperty(
            final Graph graph,
            final WorkspaceHelper workspaceHelper,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration,
            final OntologyRepository ontologyRepository
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String graphVertexId = getAttributeString(request, "graphVertexId");
        final String propertyName = getRequiredParameter(request, "propertyName");
        final String propertyKey = getRequiredParameter(request, "propertyKey");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);
        OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(propertyName);

        Vertex graphVertex = graph.getVertex(graphVertexId, authorizations);
        final List<Property> properties = new ArrayList<>();

        properties.addAll(toList(graphVertex.getProperties(propertyKey, propertyName)));
        if (ontologyProperty != null) {
            for (String dependentPropertyIri : ontologyProperty.getDependentPropertyIris()) {
                properties.addAll(toList(graphVertex.getProperties(propertyKey, dependentPropertyIri)));
            }
        }

        if (properties.size() == 0) {
            LOGGER.warn("Could not find property %s:%s on %s", propertyName, propertyKey, graphVertexId);
            respondWithNotFound(response);
            return;
        }

        // add the vertex to the workspace so that the changes show up in the diff panel
        getWorkspaceRepository().updateEntityOnWorkspace(workspaceId, graphVertexId, null, null, user);

        SandboxStatus[] sandboxStatuses = SandboxStatusUtil.getPropertySandboxStatuses(properties, workspaceId);

        for (int i = 0; i < sandboxStatuses.length; i++) {
            boolean propertyIsPublic = (sandboxStatuses[i] == SandboxStatus.PUBLIC);
            Property property = properties.get(i);
            workspaceHelper.deleteProperty(graphVertex, property, propertyIsPublic, workspaceId, Priority.HIGH, authorizations);
        }

        respondWithSuccessJson(response);
    }
}
