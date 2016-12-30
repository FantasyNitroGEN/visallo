package org.visallo.tools;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.vertexium.Element;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.ingest.graphProperty.ElementOrPropertyStatus;
import org.visallo.core.ingest.graphProperty.GraphPropertyMessage;
import org.visallo.core.ingest.graphProperty.GraphPropertyRunner;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerItem;
import org.visallo.core.model.workQueue.Priority;

@Parameters(commandDescription = "Run Graph Property Worker on a single element")
public class RunGpw extends CommandLineTool {
    private GraphPropertyRunner graphPropertyRunner;

    @Parameter(names = {"--vertexId"}, description = "The vertex id to run the GPW on")
    private String vertexId;

    @Parameter(names = {"--edgeId"}, description = "The edge id to run the GPW on")
    private String edgeId;

    @Parameter(names = {"--beforeActionTimestamp"}, description = "Timestamp to use before the action ocured")
    private Long beforeActionTimestamp = null;

    @Parameter(names = {"--priority"}, description = "Priority to run GPW with")
    private Priority priority = Priority.NORMAL;

    @Parameter(names = {"--propertyKey"}, description = "property key of the property to run on")
    private String propertyKey;

    @Parameter(names = {"--propertyName"}, description = "property name of the property to run on")
    private String propertyName;

    @Parameter(names = {"--visibilitySource"}, description = "visibility source of the property to run on")
    private String visibilitySource;

    @Parameter(names = {"--workspaceId"}, description = "workspace id to run as")
    private String workspaceId;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new RunGpw(), args);
    }

    @Override
    protected int run() throws Exception {
        if (vertexId == null && edgeId == null) {
            System.err.println("You must specify either a vertexId or edgeId");
            return -1;
        }
        if (vertexId != null && edgeId != null) {
            System.err.println("You can only specify one: vertexId or edgeId");
            return -1;
        }

        Element element;
        if (vertexId != null) {
            element = getGraph().getVertex(vertexId, getAuthorizations());
            if (element == null) {
                System.err.println("Could not find vertex with id: " + vertexId);
                return -1;
            }
        } else {
            element = getGraph().getEdge(edgeId, getAuthorizations());
            if (element == null) {
                System.err.println("Could not find edge with id: " + edgeId);
                return -1;
            }
        }

        graphPropertyRunner.prepare(getUser());
        try {
            GraphPropertyMessage message = new GraphPropertyMessage();
            message.setPriority(priority);
            message.setBeforeActionTimestamp(beforeActionTimestamp);
            if (edgeId != null) {
                message.setGraphEdgeId(new String[]{edgeId});
            }
            if (vertexId != null) {
                message.setGraphVertexId(new String[]{vertexId});
            }
            message.setTraceEnabled(true);
            message.setPropertyKey(propertyKey);
            message.setPropertyName(propertyName);
            message.setVisibilitySource(visibilitySource);
            message.setStatus(ElementOrPropertyStatus.UPDATE);
            message.setWorkspaceId(workspaceId);
            ImmutableList<Element> elements = ImmutableList.of(element);
            GraphPropertyWorkerItem workerItem = new GraphPropertyWorkerItem(message, elements);
            graphPropertyRunner.process(workerItem);
        } finally {
            graphPropertyRunner.shutdown();
        }
        return 0;
    }

    @Inject
    public void setGraphPropertyRunner(GraphPropertyRunner graphPropertyRunner) {
        this.graphPropertyRunner = graphPropertyRunner;
    }
}
