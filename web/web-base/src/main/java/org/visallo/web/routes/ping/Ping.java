package org.visallo.web.routes.ping;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.ContentType;
import com.v5analytics.webster.annotations.Handle;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.ping.PingUtil;
import org.visallo.web.parameterProviders.RemoteAddr;

public class Ping implements ParameterizedHandler {
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final LongRunningProcessRepository longRunningProcessRepository;
    private final Authorizations authorizations;
    private final PingUtil pingUtil;

    @Inject
    public Ping(
            UserRepository userRepository,
            Graph graph,
            WorkQueueRepository workQueueRepository,
            LongRunningProcessRepository longRunningProcessRepository,
            PingUtil pingUtil,
            AuthorizationRepository authorizationRepository
    ) {
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
        this.longRunningProcessRepository = longRunningProcessRepository;
        this.pingUtil = pingUtil;
        this.authorizations = authorizationRepository.getGraphAuthorizations(userRepository.getSystemUser());
    }

    @Handle
    @ContentType("text/plain")
    public PingResponse ping(@RemoteAddr String remoteAddr) {
        // test search
        long startTime = System.currentTimeMillis();
        String vertexId = pingUtil.search(graph, authorizations);
        long searchTime = System.currentTimeMillis() - startTime;

        // test retrieval
        startTime = System.currentTimeMillis();
        pingUtil.retrieve(vertexId, graph, authorizations);
        long retrievalTime = System.currentTimeMillis() - startTime;

        // test save
        startTime = System.currentTimeMillis();
        Vertex pingVertex = pingUtil.createVertex(remoteAddr, searchTime, retrievalTime, graph, authorizations);
        long saveTime = System.currentTimeMillis() - startTime;

        // test queues (and asynchronously test GPW and LRP)
        startTime = System.currentTimeMillis();
        pingUtil.enqueueToWorkQueue(pingVertex, workQueueRepository, Priority.HIGH);
        pingUtil.enqueueToLongRunningProcess(pingVertex, longRunningProcessRepository, authorizations);
        long enqueueTime = System.currentTimeMillis() - startTime;

        return new PingResponse(searchTime, retrievalTime, saveTime, enqueueTime);
    }
}
