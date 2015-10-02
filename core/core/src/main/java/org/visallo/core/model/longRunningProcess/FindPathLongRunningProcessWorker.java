package org.visallo.core.model.longRunningProcess;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Path;
import org.vertexium.ProgressCallback;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiVertexFindPathResponse;

import java.util.ArrayList;
import java.util.List;

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
        String[] labels = findPath.getLabels();
        int hops = findPath.getHops();

        ClientApiVertexFindPathResponse results = new ClientApiVertexFindPathResponse();
        ProgressCallback progressCallback = new ProgressCallback() {
            @Override
            public void progress(double progressPercent, Step step, Integer edgeIndex, Integer vertexCount) {
                longRunningProcessRepository.reportProgress(longRunningProcessQueueItem, progressPercent, step.formatMessage(edgeIndex, vertexCount));
            }
        };
        Iterable<Path> paths = graph.findPaths(findPath.getOutVertexId(), findPath.getInVertexId(), labels, hops, progressCallback, authorizations);
        for (Path path : paths) {
            List<String> clientApiVertexPath = new ArrayList<>();
            for (String s : path) {
                clientApiVertexPath.add(s);
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
