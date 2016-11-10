package org.visallo.tools.migrations;

import com.beust.jcommander.Parameters;
import org.json.JSONObject;
import org.vertexium.*;
import org.visallo.core.bootstrap.VisalloBootstrap;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workspace.WorkspaceProperties;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.EnumSet;

import static org.visallo.core.util.StreamUtil.stream;
import static org.visallo.core.model.workspace.WorkspaceProperties.WORKSPACE_TO_PRODUCT_RELATIONSHIP_IRI;

@Parameters(commandDescription = "Migrate workspaces to work products")
public class WorkspacesToWorkProduct extends CommandLineTool {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspacesToWorkProduct.class);

    private static final String VISALLO_GRAPH_VERSION = "visallo.graph.version";
    private static final String GRAPH_KIND = "org.visallo.web.product.graph.GraphWorkProduct";
    private static final String EDGE_POSITION = "http://visallo.org/workspace/product/graph#entityPosition";
    private static final String GRAPH_VISIBLE = "http://visallo.org/workspace#toEntity/visible";
    private static final String GRAPH_POSITION_X = "http://visallo.org/workspace#toEntity/graphPositionX";
    private static final String GRAPH_POSITION_Y = "http://visallo.org/workspace#toEntity/graphPositionY";
    private static final String PRODUCT_TO_ENTITY_RELATIONSHIP_IRI = "http://visallo.org/workspace/product#toEntity";
    private static final Integer VISALLO_GRAPH_VERSION_NEEDS_MIGRATION = 1;
    private static final Integer VISALLO_GRAPH_VERSION_BUMP = 2;

    private Graph graph = null;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new WorkspacesToWorkProduct(), args, false);
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
                if (VISALLO_GRAPH_VERSION_BUMP.equals(visalloGraphVersion)) {
                    throw new VisalloException("Migration has already completed. Graph version: " + visalloGraphVersion);
                } else if (!VISALLO_GRAPH_VERSION_NEEDS_MIGRATION.equals(visalloGraphVersion)) {
                    throw new VisalloException("Migration can only run from version " + VISALLO_GRAPH_VERSION_NEEDS_MIGRATION +
                            ". Current graph version = " + visalloGraphVersion);
                }
            }

            Authorizations authorizations = graph.createAuthorizations(VisalloVisibility.SUPER_USER_VISIBILITY_STRING);
            Iterable<Vertex> vertices = getGraph().getVertices(EnumSet.of(FetchHint.OUT_EDGE_LABELS), authorizations);

            stream(vertices)
                    .filter(vertex ->
                            stream(vertex.getEdgeLabels(Direction.OUT, authorizations))
                                    .anyMatch(s -> s.equals(WorkspaceRepository.WORKSPACE_TO_USER_RELATIONSHIP_IRI))
                    )
                    .forEach(workspace -> {
                        LOGGER.info("Found a workspace: %s", workspace.getId());

                        VertexBuilder productVertex = graph.prepareVertex(workspace.getVisibility());
                        VisalloProperties.CONCEPT_TYPE.setProperty(
                                productVertex,
                                WorkspaceProperties.PRODUCT_CONCEPT_IRI,
                                workspace.getVisibility()
                        );
                        WorkspaceProperties.TITLE.setProperty(productVertex, "Migrated", workspace.getVisibility());
                        WorkspaceProperties.PRODUCT_KIND.setProperty(productVertex, GRAPH_KIND, workspace.getVisibility());
                        String productId = productVertex.getVertexId();
                        productVertex.save(authorizations);
                        EdgeBuilderByVertexId workspaceToProductEdge = graph.prepareEdge(workspace.getId() + "_hasProduct_" + productId, workspace.getId(), productId,
                                WORKSPACE_TO_PRODUCT_RELATIONSHIP_IRI, workspace.getVisibility());

                        workspaceToProductEdge.save(authorizations);
                        LOGGER.info("Creating product: %s", productId);


                        stream(workspace.getEdgeInfos(Direction.OUT, authorizations))
                                .forEach(edgeInfo -> {
                                    if (edgeInfo.getLabel().equals(WorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI)) {

                                        Edge edge = getGraph().getEdge(edgeInfo.getEdgeId(), authorizations);
                                        if (edge != null && edge.getPropertyValue(GRAPH_VISIBLE).equals(true)) {
                                            LOGGER.info("\tCreating entity: %s", edgeInfo.getVertexId());

                                            String edgeId = productId + "_hasVertex_" + edgeInfo.getVertexId();
                                            EdgeBuilderByVertexId productToEntityEdge = graph.prepareEdge(
                                                    edgeId,
                                                    productId,
                                                    edgeInfo.getVertexId(),
                                                    PRODUCT_TO_ENTITY_RELATIONSHIP_IRI,
                                                    workspace.getVisibility()
                                            );
                                            JSONObject position = new JSONObject();
                                            position.put("x", positionForProperty(edge, GRAPH_POSITION_X));
                                            position.put("y", positionForProperty(edge, GRAPH_POSITION_Y));
                                            productToEntityEdge.setProperty(EDGE_POSITION, position.toString(), workspace.getVisibility());
                                            productToEntityEdge.save(authorizations);
                                        }
                                    }
                                });
                    });

            graph.setMetadata(VISALLO_GRAPH_VERSION, VISALLO_GRAPH_VERSION_BUMP);
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

    private int positionForProperty(Edge edge, String name) {
        Integer val = (Integer) edge.getPropertyValue(name);
        if (val == null) return 0;
        return val;
    }
}

