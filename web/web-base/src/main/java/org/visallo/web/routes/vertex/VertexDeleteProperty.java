package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.SandboxStatusUtil;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.clientapi.model.SandboxStatus;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.ArrayList;
import java.util.List;

import static org.vertexium.util.IterableUtils.toList;

public class VertexDeleteProperty implements ParameterizedHandler {
    private final Graph graph;
    private final WorkspaceHelper workspaceHelper;
    private final OntologyRepository ontologyRepository;
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public VertexDeleteProperty(
            final Graph graph,
            final WorkspaceHelper workspaceHelper,
            final OntologyRepository ontologyRepository,
            final WorkspaceRepository workspaceRepository
    ) {
        this.graph = graph;
        this.workspaceHelper = workspaceHelper;
        this.ontologyRepository = ontologyRepository;
        this.workspaceRepository = workspaceRepository;
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
        final List<Property> properties = new ArrayList<>();

        properties.addAll(toList(graphVertex.getProperties(propertyKey, propertyName)));
        if (ontologyProperty != null) {
            for (String dependentPropertyIri : ontologyProperty.getDependentPropertyIris()) {
                properties.addAll(toList(graphVertex.getProperties(propertyKey, dependentPropertyIri)));
            }
        }

        if (properties.size() == 0) {
            throw new VisalloResourceNotFoundException(String.format("Could not find property %s:%s on %s", propertyName, propertyKey, graphVertexId));
        }

        // add the vertex to the workspace so that the changes show up in the diff panel
        workspaceRepository.updateEntityOnWorkspace(workspaceId, graphVertexId, null, null, user);

        SandboxStatus[] sandboxStatuses = SandboxStatusUtil.getPropertySandboxStatuses(properties, workspaceId);

        for (int i = 0; i < sandboxStatuses.length; i++) {
            boolean propertyIsPublic = (sandboxStatuses[i] == SandboxStatus.PUBLIC);
            Property property = properties.get(i);
            workspaceHelper.deleteProperty(graphVertex, property, propertyIsPublic, workspaceId, Priority.HIGH, authorizations);
        }

        return VisalloResponse.SUCCESS;
    }
}
