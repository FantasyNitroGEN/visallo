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
    private final UserRepository userRepository;
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final LongRunningProcessRepository longRunningProcessRepository;

    @Inject
    public Ping(
            UserRepository userRepository,
            Graph graph,
            WorkQueueRepository workQueueRepository,
            LongRunningProcessRepository longRunningProcessRepository,
            AuthorizationRepository authorizationRepository
    ) {
        this.userRepository = userRepository;
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
        this.longRunningProcessRepository = longRunningProcessRepository;

        PingUtil.setup(authorizationRepository, userRepository);
    }

    @Handle
    @ContentType("text/plain")
    public String ping(
            @RemoteAddr String remoteAddr
    ) {
        Authorizations authorizations = userRepository.getAuthorizations(userRepository.getSystemUser());

        // test search
        long startTime = System.currentTimeMillis();
        String vertexId = PingUtil.search(graph, authorizations);
        long searchTime = System.currentTimeMillis() - startTime;

        // test retrieval
        startTime = System.currentTimeMillis();
        PingUtil.retrieve(vertexId, graph, authorizations);
        long retrievalTime = System.currentTimeMillis() - startTime;

        // test save
        startTime = System.currentTimeMillis();
        Vertex pingVertex = PingUtil.createVertex(remoteAddr, searchTime, retrievalTime, graph, authorizations);
        long saveTime = System.currentTimeMillis() - startTime;

        // test queues (and asynchronously test GPW and LRP)
        startTime = System.currentTimeMillis();
        PingUtil.enqueue(pingVertex, workQueueRepository, Priority.HIGH);
        PingUtil.enqueue(pingVertex, longRunningProcessRepository, PingUtil.getUser(userRepository), authorizations);
        long enqueueTime = System.currentTimeMillis() - startTime;

        return String.format("ok (search: %dms, retrieval: %dms, save: %dms, enqueue: %dms)", searchTime, retrievalTime, saveTime, enqueueTime);
    }
}
