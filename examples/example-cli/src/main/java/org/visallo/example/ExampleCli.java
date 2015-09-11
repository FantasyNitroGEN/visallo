package org.visallo.example;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.tinkerpop.blueprints.Vertex;
import org.vertexium.Authorizations;
import org.vertexium.GraphFactory;
import org.vertexium.accumulo.AccumuloGraph;
import org.vertexium.accumulo.blueprints.AccumuloVertexiumBlueprintsGraph;
import org.vertexium.blueprints.AuthorizationsProvider;
import org.vertexium.blueprints.DefaultVisibilityProvider;
import org.vertexium.blueprints.VisibilityProvider;
import org.vertexium.util.ConfigurationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExampleCli {
    @Parameter(names = {"--help", "-h"}, description = "Prints help")
    private boolean help;

    @Parameter(names = {"--config", "-c"}, required = true, description = "Configuration file names")
    private List<String> configFileNames;

    @Parameter(names = {"--configprefix", "-cp"}, description = "Graph configuration prefix")
    private String configPropertyPrefix;

    @Parameter(names = {"--auth", "-a"}, description = "Authorizations")
    private List<String> auths = new ArrayList<String>();

    public static void main(String[] args) throws Exception {
        int ret = new ExampleCli().run(args);
        System.exit(ret);
    }

    private int run(String[] args) throws Exception {
        JCommander cmd = new JCommander(this, args);
        if (help) {
            cmd.usage();
            return -1;
        }

        Map<String, String> config = ConfigurationUtils.loadConfig(configFileNames, configPropertyPrefix);
        AccumuloGraph graph = createAccumuloGraph(config);
        try {
            AccumuloVertexiumBlueprintsGraph blueprintsGraph = createBlueprintsGraph(graph, config);
            runWithGraph(blueprintsGraph);
            return 0;
        } finally {
            graph.shutdown();
        }
    }

    private void runWithGraph(AccumuloVertexiumBlueprintsGraph blueprintsGraph) {
        Iterable<Vertex> vertices = blueprintsGraph.query().has("http://visallo.org#conceptType", "http://visallo.org/dev#person").vertices();
        for (Vertex vertex : vertices) {
            System.out.println(vertex.getId());
        }
    }

    private AccumuloGraph createAccumuloGraph(Map<String, String> config) {
        return (AccumuloGraph) new GraphFactory().createGraph(config);
    }

    private AccumuloVertexiumBlueprintsGraph createBlueprintsGraph(AccumuloGraph graph, Map<String, String> config) {
        final Authorizations authorizations = graph.createAuthorizations(auths);
        VisibilityProvider visibilityProvider = new DefaultVisibilityProvider(config);
        AuthorizationsProvider authorizationProvider = new AuthorizationsProvider() {
            public Authorizations getAuthorizations() {
                return authorizations;
            }
        };
        return new AccumuloVertexiumBlueprintsGraph(graph, visibilityProvider, authorizationProvider);
    }
}
