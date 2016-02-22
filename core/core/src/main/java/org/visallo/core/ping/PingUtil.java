package org.visallo.core.ping;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.query.*;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.Privilege;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

public class PingUtil {
    public static final String VISIBILITY_STRING = "ping";
    public static final Visibility VISIBILITY = new VisalloVisibility(VISIBILITY_STRING).getVisibility();
    private static final String USERNAME = "ping";
    private static final String DISPLAY_NAME = "Ping User";

    public static void setup(AuthorizationRepository authorizationRepository, UserRepository userRepository) {
        authorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
        userRepository.findOrAddUser(USERNAME, DISPLAY_NAME, null, UserRepository.createRandomPassword(), Sets.newHashSet(Privilege.READ), Sets.newHashSet(VISIBILITY_STRING));
    }

    public static User getUser(UserRepository userRepository) {
        return userRepository.findByUsername(USERNAME);
    }

    public static String search(Graph graph, Authorizations authorizations) {
        Query query = graph.query(authorizations).limit(1);
        List<Vertex> vertices = Lists.newArrayList(query.vertices());
        if (vertices.size() == 0) {
            throw new VisalloException("query returned no vertices");
        } else if (vertices.size() > 1) {
            throw new VisalloException("query returned more than one vertex");
        }
        return vertices.get(0).getId();
    }

    public static void retrieve(String vertexId, Graph graph, Authorizations authorizations) {
        Vertex retrievedVertex = graph.getVertex(vertexId, authorizations);
        if (retrievedVertex == null) {
            throw new VisalloException("failed to retrieve vertex by id: " + vertexId);
        }
    }

    public static Vertex createVertex(String remoteAddr, long searchTime, long retrievalTime, Graph graph, Authorizations authorizations) {
        Date createDate = new Date();
        String vertexId = PingOntology.getVertexId(createDate);
        ElementMutation<Vertex> mutation = graph.prepareVertex(vertexId, VISIBILITY);
        VisalloProperties.CONCEPT_TYPE.setProperty(mutation, PingOntology.IRI_CONCEPT_PING, VISIBILITY);
        PingOntology.CREATE_DATE.setProperty(mutation, createDate, VISIBILITY);
        PingOntology.CREATE_REMOTE_ADDR.setProperty(mutation, remoteAddr, VISIBILITY);
        PingOntology.SEARCH_TIME_MS.setProperty(mutation, searchTime, VISIBILITY);
        PingOntology.RETRIEVAL_TIME_MS.setProperty(mutation, retrievalTime, VISIBILITY);
        Vertex vertex = mutation.save(authorizations);
        graph.flush();
        return vertex;
    }

    public static void enqueue(Vertex vertex, WorkQueueRepository workQueueRepository, Priority priority) {
        workQueueRepository.pushElement(vertex, priority);
    }

    public static void gpwUpdate(Vertex vertex, Graph graph, Authorizations authorizations) {
        Date updateDate = new Date();
        Long waitTimeMs = updateDate.getTime() - PingOntology.CREATE_DATE.getPropertyValue(vertex).getTime();
        ElementMutation<Vertex> mutation = vertex.prepareMutation();
        PingOntology.GRAPH_PROPERTY_WORKER_DATE.setProperty(mutation, updateDate, VISIBILITY);
        PingOntology.GRAPH_PROPERTY_WORKER_HOSTNAME.setProperty(mutation, getHostname(), VISIBILITY);
        PingOntology.GRAPH_PROPERTY_WORKER_HOST_ADDRESS.setProperty(mutation, getHostAddress(), VISIBILITY);
        PingOntology.GRAPH_PROPERTY_WORKER_WAIT_TIME_MS.setProperty(mutation, waitTimeMs, VISIBILITY);
        mutation.save(authorizations);
        graph.flush();
    }

    public static void enqueue(Vertex vertex, LongRunningProcessRepository longRunningProcessRepository, User user, Authorizations authorizations) {
        longRunningProcessRepository.enqueue(new PingLongRunningProcessQueueItem(vertex).toJson(), user, authorizations);
    }

    public static void lrpUpdate(Vertex vertex, Graph graph, Authorizations authorizations) {
        Date updateDate = new Date();
        Long waitTimeMs = updateDate.getTime() - PingOntology.CREATE_DATE.getPropertyValue(vertex).getTime();
        ElementMutation<Vertex> mutation = vertex.prepareMutation();
        PingOntology.LONG_RUNNING_PROCESS_DATE.setProperty(mutation, updateDate, VISIBILITY);
        PingOntology.LONG_RUNNING_PROCESS_HOSTNAME.setProperty(mutation, getHostname(), VISIBILITY);
        PingOntology.LONG_RUNNING_PROCESS_HOST_ADDRESS.setProperty(mutation, getHostAddress(), VISIBILITY);
        PingOntology.LONG_RUNNING_PROCESS_WAIT_TIME_MS.setProperty(mutation, waitTimeMs, VISIBILITY);
        mutation.save(authorizations);
        graph.flush();
    }

    public static JSONObject getAverages(int minutes, Graph graph, Authorizations authorizations) {
        Date minutesAgo = new Date(System.currentTimeMillis() - minutes * 60 * 1000);
        Query q = graph.query(authorizations)
                .has(VisalloProperties.CONCEPT_TYPE.getPropertyName(), PingOntology.IRI_CONCEPT_PING)
                .has(PingOntology.CREATE_DATE.getPropertyName(), Compare.GREATER_THAN, minutesAgo)
                .limit(0);
        if (!(q instanceof GraphQueryWithStatisticsAggregation)) {
            throw new VisalloException("Cannot get statistics from query " + q.getClass().getName());
        }
        GraphQueryWithStatisticsAggregation qWithAgg = (GraphQueryWithStatisticsAggregation) q;
        qWithAgg.addStatisticsAggregation(PingOntology.SEARCH_TIME_MS.getPropertyName(), PingOntology.SEARCH_TIME_MS.getPropertyName());
        qWithAgg.addStatisticsAggregation(PingOntology.RETRIEVAL_TIME_MS.getPropertyName(), PingOntology.RETRIEVAL_TIME_MS.getPropertyName());
        qWithAgg.addStatisticsAggregation(PingOntology.GRAPH_PROPERTY_WORKER_WAIT_TIME_MS.getPropertyName(), PingOntology.GRAPH_PROPERTY_WORKER_WAIT_TIME_MS.getPropertyName());
        qWithAgg.addStatisticsAggregation(PingOntology.LONG_RUNNING_PROCESS_WAIT_TIME_MS.getPropertyName(), PingOntology.LONG_RUNNING_PROCESS_WAIT_TIME_MS.getPropertyName());
        Iterable<Vertex> vertices = q.vertices();
        if (!(vertices instanceof IterableWithStatisticsResults)) {
            throw new VisalloException("Cannot get statistics from results " + q.getClass().getName());
        }
        IterableWithStatisticsResults verticesWithAgg = (IterableWithStatisticsResults) vertices;
        StatisticsResult searchTimeAgg = verticesWithAgg.getStatisticsResults(PingOntology.SEARCH_TIME_MS.getPropertyName());
        StatisticsResult retrievalTimeAgg = verticesWithAgg.getStatisticsResults(PingOntology.RETRIEVAL_TIME_MS.getPropertyName());
        StatisticsResult gpwWaitTimeAgg = verticesWithAgg.getStatisticsResults(PingOntology.GRAPH_PROPERTY_WORKER_WAIT_TIME_MS.getPropertyName());
        StatisticsResult lrpWaitTimeAgg = verticesWithAgg.getStatisticsResults(PingOntology.LONG_RUNNING_PROCESS_WAIT_TIME_MS.getPropertyName());

        JSONObject json = new JSONObject();
        json.put("pingCount", searchTimeAgg.getCount());
        json.put("averageSearchTime", searchTimeAgg.getAverage());
        json.put("averageRetrievalTime", retrievalTimeAgg.getAverage());
        json.put("graphPropertyWorkerCount", gpwWaitTimeAgg.getCount());
        json.put("averageGraphPropertyWorkerWaitTime", gpwWaitTimeAgg.getAverage());
        json.put("longRunningProcessCount", lrpWaitTimeAgg.getCount());
        json.put("averageLongRunningProcessWaitTime", lrpWaitTimeAgg.getAverage());
        return json;
    }

    private static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // do nothing
        }
        return "";
    }

    private static String getHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            // do nothing
        }
        return "";
    }
}
