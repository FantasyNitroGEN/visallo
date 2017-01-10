package org.visallo.tools.migrations;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.vertexium.*;
import org.visallo.core.bootstrap.VisalloBootstrap;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

@Parameters(commandDescription = "Update Visallo metadata graph version")
public class GraphVersionBump extends CommandLineTool {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GraphVersionBump.class);
    private static final String VISALLO_GRAPH_VERSION = "visallo.graph.version";
    private Graph graph = null;

    @Parameter(required = true, names = {"--graph-version"}, description = "Specify the metadata graph version to set.")
    private Integer toVersion;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new GraphVersionBump(), args, false);
    }

    @Override
    protected int run() throws Exception {
        VisalloBootstrap bootstrap = VisalloBootstrap.bootstrap(getConfiguration());
        graph = getGraph();

        try {
            Object visalloGraphVersionObj = graph.getMetadata(VISALLO_GRAPH_VERSION);

            if (visalloGraphVersionObj == null) {
                throw new VisalloException("No graph metadata version set");
            } else if (visalloGraphVersionObj instanceof Integer) {
                Integer visalloGraphVersion = (Integer) visalloGraphVersionObj;
                if (toVersion.equals(visalloGraphVersion)) {
                    return 0;
                }
            }

            graph.setMetadata(VISALLO_GRAPH_VERSION, toVersion);
            return 0;
        } finally {
            graph.shutdown();
        }
    }

    @Override
    public Graph getGraph() {
        if (graph == null) {
            GraphFactory factory = new GraphFactory();
            graph = factory.createGraph(getConfiguration().getSubset(Configuration.GRAPH_PROVIDER));
        }
        return graph;
    }
}

