package org.visallo.web.product.map.routes;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.graph.GraphUpdateContext;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.JSONUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiWorkspace;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.SourceGuid;
import org.visallo.web.product.map.MapWorkProduct;

public class UpdateVertices implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(UpdateVertices.class);

    private final Graph graph;
    private final VisibilityTranslator visibilityTranslator;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceHelper workspaceHelper;
    private final WorkQueueRepository workQueueRepository;
    private final OntologyRepository ontologyRepository;
    private final AuthorizationRepository authorizationRepository;
    private final GraphRepository graphRepository;

    @Inject
    public UpdateVertices(
            Graph graph,
            VisibilityTranslator visibilityTranslator,
            WorkspaceRepository workspaceRepository,
            WorkspaceHelper workspaceHelper,
            WorkQueueRepository workQueueRepository,
            OntologyRepository ontologyRepository,
            AuthorizationRepository authorizationRepository,
            GraphRepository graphRepository
    ) {
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
        this.workspaceRepository = workspaceRepository;
        this.workspaceHelper = workspaceHelper;
        this.workQueueRepository = workQueueRepository;
        this.ontologyRepository = ontologyRepository;
        this.authorizationRepository = authorizationRepository;
        this.graphRepository = graphRepository;
    }

    @Handle
    public void handle(
            @Required(name = "updates") String updates,
            @Required(name = "productId") String productId,
            @ActiveWorkspaceId String workspaceId,
            @SourceGuid String sourceGuid,
            User user,
            VisalloResponse response
    ) throws Exception {
        JSONObject updateVertices = new JSONObject(updates);
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                user,
                WorkspaceRepository.VISIBILITY_STRING,
                workspaceId
        );

        workspaceHelper.updateEntitiesOnWorkspace(
                workspaceId,
                JSONUtil.toStringList(updateVertices.names()),
                user
        );

        try (GraphUpdateContext ctx = graphRepository.beginGraphUpdate(Priority.NORMAL, user, authorizations)) {
            MapWorkProduct mapWorkProduct = new MapWorkProduct(ontologyRepository, authorizationRepository);
            Vertex productVertex = graph.getVertex(productId, authorizations);

            mapWorkProduct.updateVertices(ctx, productVertex, updateVertices, visibilityTranslator.getDefaultVisibility());
        } catch(Exception e) {
            throw new RuntimeException(e);
        }

        Workspace workspace = workspaceRepository.findById(workspaceId, user);
        ClientApiWorkspace clientApiWorkspace = workspaceRepository.toClientApi(workspace, user, authorizations);

        workQueueRepository.broadcastWorkProductChange(productId, clientApiWorkspace, user, sourceGuid);

        response.respondWithSuccessJson();
    }
}
