package org.visallo.tools;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.inject.Inject;
import org.vertexium.GraphWithSearchIndex;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.model.graph.GraphReindexService;

@Parameters(commandDescription = "Reindex elements")
public class Reindex extends CommandLineTool {
    private GraphReindexService graphReindexService;

    @Parameter(names = {"--vertices", "-v"}, description = "Include all vertices")
    private boolean vertices = false;

    @Parameter(names = {"--edges", "-e"}, description = "Include all edges")
    private boolean edges = false;

    @Parameter(names = {"--all", "-a"}, description = "Include all elements")
    private boolean all = false;

    @Parameter(names = {"--batchSize"}, description = "Batch size of elements to send for reindexing")
    private int batchSize = 100;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new Reindex(), args);
    }

    @Override
    protected int run() throws Exception {
        if (!(getGraph() instanceof GraphWithSearchIndex)) {
            System.err.println("Graph must extend " + GraphWithSearchIndex.class.getName() + " to support reindexing");
            return -1;
        }

        if (!vertices && !edges && !all) {
            System.err.println("You must specify something to index (--vertices, --edges, or --all)");
            return -1;
        }

        if (vertices || all) {
            graphReindexService.reindexVertices(batchSize, getAuthorizations());
        }

        if (edges || all) {
            graphReindexService.reindexEdges(batchSize, getAuthorizations());
        }

        return 0;
    }

    @Inject
    public void setGraphReindexService(GraphReindexService graphReindexService) {
        this.graphReindexService = graphReindexService;
    }
}
