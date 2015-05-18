package org.visallo.core.model.longRunningProcess;

import com.google.inject.Inject;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiVertex;
import org.visallo.web.clientapi.model.ClientApiVertexFindPathResponse;
import org.json.JSONObject;
import org.vertexium.*;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Name("Find Path")
@Description("Finds a path between two vertices")
public class FindPathLongRunningProcessWorker extends LongRunningProcessWorker {
    private Graph graph;
    private LongRunningProcessRepository longRunningProcessRepository;

    @Override
    public boolean isHandled(JSONObject longRunningProcessQueueItem) {
        return longRunningProcessQueueItem.getString("type").equals("findPath");
    }

    @Override
    public void processInternal(final JSONObject longRunningProcessQueueItem) {
        FindPathLongRunningProcessQueueItem findPath = ClientApiConverter.toClientApi(longRunningProcessQueueItem.toString(), FindPathLongRunningProcessQueueItem.class);

        Authorizations authorizations = getAuthorizations(findPath.getAuthorizations());
        Vertex sourceVertex = this.graph.getVertex(findPath.getSourceVertexId(), authorizations);
        checkNotNull(sourceVertex, "Could not find source vertex: " + findPath.getSourceVertexId());
        Vertex destVertex = this.graph.getVertex(findPath.getDestVertexId(), authorizations);
        checkNotNull(destVertex, "Could not find destination vertex: " + findPath.getDestVertexId());
        int hops = findPath.getHops();
        String workspaceId = findPath.getWorkspaceId();

        ClientApiVertexFindPathResponse results = new ClientApiVertexFindPathResponse();
        ProgressCallback progressCallback = new ProgressCallback() {
            @Override
            public void progress(double progressPercent, Step step, Integer edgeIndex, Integer vertexCount) {
                longRunningProcessRepository.reportProgress(longRunningProcessQueueItem, progressPercent, step.formatMessage(edgeIndex, vertexCount));
            }
        };
        Iterable<Path> paths = graph.findPaths(sourceVertex, destVertex, hops, progressCallback, authorizations);
        for (Path path : paths) {
            List<ClientApiElement> clientApiElementPath = ClientApiConverter.toClientApi(graph.getVerticesInOrder(path, authorizations), workspaceId, authorizations);
            List<ClientApiVertex> clientApiVertexPath = new ArrayList<>();
            for (ClientApiElement e : clientApiElementPath) {
                clientApiVertexPath.add((ClientApiVertex) e);
            }
            results.getPaths().add(clientApiVertexPath);
        }

        String resultsString = ClientApiConverter.clientApiToString(results);
        JSONObject resultsJson = new JSONObject(resultsString);
        longRunningProcessQueueItem.put("results", resultsJson);
        longRunningProcessQueueItem.put("resultsCount", results.getPaths().size());
    }

    private Authorizations getAuthorizations(String[] authorizations) {
        return graph.createAuthorizations(authorizations);
    }

    @Inject
    public void setLongRunningProcessRepository(LongRunningProcessRepository longRunningProcessRepository) {
        this.longRunningProcessRepository = longRunningProcessRepository;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }
}
