package org.visallo.web.product.graph.routes;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.graph.GraphUpdateContext;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiWorkspace;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.SourceGuid;
import org.visallo.web.product.graph.GraphWorkProduct;

public class CollapseVertices implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(CollapseVertices.class);

    private final Graph graph;
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private final OntologyRepository ontologyRepository;
    private final AuthorizationRepository authorizationRepository;
    private final GraphRepository graphRepository;
    private final UserRepository userRepository;

    @Inject
    public CollapseVertices(
            Graph graph,
            WorkspaceRepository workspaceRepository,
            WorkQueueRepository workQueueRepository,
            OntologyRepository ontologyRepository,
            AuthorizationRepository authorizationRepository,
            GraphRepository graphRepository,
            UserRepository userRepository
    ) {
        this.graph = graph;
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
        this.ontologyRepository = ontologyRepository;
        this.authorizationRepository = authorizationRepository;
        this.graphRepository = graphRepository;
        this.userRepository = userRepository;
    }

    @Handle
    public JSONObject handle(
            @Optional(name = "vertexId") String vertexId,
            @Required(name = "params") String paramsStr,
            @Required(name = "productId") String productId,
            @ActiveWorkspaceId String workspaceId,
            @SourceGuid String sourceGuid,
            User user
    ) throws Exception {
        JSONObject params = new JSONObject(paramsStr);
        JSONObject nodeJson;

        if (!workspaceRepository.hasWritePermissions(workspaceId, user)) {
            throw new VisalloAccessDeniedException(
                    "user " + user.getUserId() + " does not have write access to workspace " + workspaceId,
                    user,
                    workspaceId
            );
        }

        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                user,
                WorkspaceRepository.VISIBILITY_STRING,
                workspaceId
        );

        try (GraphUpdateContext ctx = graphRepository.beginGraphUpdate(Priority.HIGH, user, authorizations)) {
            GraphWorkProduct graphWorkProduct = new GraphWorkProduct(ontologyRepository, authorizationRepository, graphRepository, userRepository);
            Vertex productVertex = graph.getVertex(productId, authorizations);

            params.putOpt("id", vertexId);

            nodeJson = graphWorkProduct.addCompoundNode(ctx, productVertex, params, user, WorkspaceRepository.VISIBILITY.getVisibility(), authorizations);
        } catch(Exception e) {
            throw new VisalloException("Could not collapse vertices in product: " + productId);
        }

        Workspace workspace = workspaceRepository.findById(workspaceId, user);
        ClientApiWorkspace clientApiWorkspace = workspaceRepository.toClientApi(workspace, user, authorizations);

        workQueueRepository.broadcastWorkProductChange(productId, clientApiWorkspace, user, sourceGuid);

        return nodeJson;
    }
}
