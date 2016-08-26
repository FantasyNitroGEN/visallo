package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.JsonSerializer;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.JustificationText;
import org.visallo.web.util.VisibilityValidator;

import java.util.ResourceBundle;

public class EdgeCreate implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(EdgeCreate.class);

    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final GraphRepository graphRepository;
    private final VisibilityTranslator visibilityTranslator;

    @Inject
    public EdgeCreate(
            Graph graph,
            WorkQueueRepository workQueueRepository,
            GraphRepository graphRepository,
            VisibilityTranslator visibilityTranslator
    ) {
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
        this.graphRepository = graphRepository;
        this.visibilityTranslator = visibilityTranslator;
    }

    @Handle
    public ClientApiElement handle(
            @Optional(name = "edgeId") String edgeId,
            @Required(name = "outVertexId") String outVertexId,
            @Required(name = "inVertexId") String inVertexId,
            @Required(name = "predicateLabel") String predicateLabel,
            @Required(name = "visibilitySource") String visibilitySource,
            @JustificationText String justificationText,
            ClientApiSourceInfo sourceInfo,
            @ActiveWorkspaceId String workspaceId,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Vertex inVertex = graph.getVertex(inVertexId, authorizations);
        Vertex outVertex = graph.getVertex(outVertexId, authorizations);

        VisibilityValidator.validate(graph, visibilityTranslator, resourceBundle, visibilitySource, user, authorizations);

        Edge edge = graphRepository.addEdge(
                edgeId,
                outVertex,
                inVertex,
                predicateLabel,
                justificationText,
                sourceInfo,
                visibilitySource,
                workspaceId,
                user,
                authorizations
        );

        graph.flush();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Statement created:\n" + JsonSerializer.toJson(edge, workspaceId, authorizations).toString(2));
        }

        workQueueRepository.broadcastElement(edge, workspaceId);
        workQueueRepository.pushElement(edge, Priority.HIGH);
        return ClientApiConverter.toClientApi(edge, workspaceId, authorizations);
    }
}
