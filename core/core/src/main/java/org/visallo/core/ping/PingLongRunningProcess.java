package org.visallo.core.ping;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.longRunningProcess.LongRunningProcessWorker;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.util.ClientApiConverter;

@Name("Ping")
@Description("run on special Ping vertices to measure LRP wait time")
public class PingLongRunningProcess extends LongRunningProcessWorker {
    private final UserRepository userRepository;
    private final Graph graph;
    private final PingUtil pingUtil;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public PingLongRunningProcess(
            AuthorizationRepository authorizationRepository,
            UserRepository userRepository,
            Graph graph,
            PingUtil pingUtil
    ) {
        this.authorizationRepository = authorizationRepository;
        this.userRepository = userRepository;
        this.graph = graph;
        this.pingUtil = pingUtil;
    }

    @Override
    protected void processInternal(JSONObject jsonObject) {
        PingLongRunningProcessQueueItem queueItem = ClientApiConverter.toClientApi(
                jsonObject.toString(),
                PingLongRunningProcessQueueItem.class
        );
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(userRepository.getSystemUser());
        Vertex vertex = graph.getVertex(queueItem.getVertexId(), authorizations);
        pingUtil.lrpUpdate(vertex, graph, authorizations);
    }

    @Override
    public boolean isHandled(JSONObject jsonObject) {
        return PingLongRunningProcessQueueItem.isHandled(jsonObject);
    }
}
