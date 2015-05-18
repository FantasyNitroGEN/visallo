package org.visallo.reindexmr;

import com.google.inject.Inject;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.vertexium.ElementType;
import org.vertexium.Graph;
import org.vertexium.accumulo.AccumuloGraph;
import org.vertexium.accumulo.mapreduce.AccumuloEdgeInputFormat;
import org.vertexium.accumulo.mapreduce.AccumuloVertexInputFormat;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.vertexium.mapreduce.VisalloMRBase;

public class ReindexMR extends VisalloMRBase {
    private static VisalloLogger LOGGER;
    private AccumuloGraph graph;
    private ElementType elementType;

    public static void main(String[] args) throws Exception {
        LOGGER = VisalloLoggerFactory.getLogger(ReindexMR.class);
        int res = ToolRunner.run(new Configuration(), new ReindexMR(), args);
        System.exit(res);
    }

    @Override
    protected void setupJob(Job job) throws Exception {
        String[] authorizations = new String[]{
                VisalloVisibility.SUPER_USER_VISIBILITY_STRING,
                OntologyRepository.VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING,
                WorkspaceRepository.VISIBILITY_STRING,
                TermMentionRepository.VISIBILITY_STRING
        };

        job.getConfiguration().setBoolean("mapred.map.tasks.speculative.execution", false);
        job.getConfiguration().setBoolean("mapred.reduce.tasks.speculative.execution", false);

        job.setJarByClass(ReindexMR.class);
        job.setMapperClass(ReindexMRMapper.class);
        job.setOutputFormatClass(NullOutputFormat.class);
        job.setNumReduceTasks(0);

        if (elementType == ElementType.VERTEX) {
            job.setInputFormatClass(AccumuloVertexInputFormat.class);
            AccumuloVertexInputFormat.setInputInfo(job, graph, getInstanceName(), getZooKeepers(), getPrincipal(), getAuthorizationToken(), authorizations);
        } else if (elementType == ElementType.EDGE) {
            job.setInputFormatClass(AccumuloEdgeInputFormat.class);
            AccumuloEdgeInputFormat.setInputInfo(job, graph, getInstanceName(), getZooKeepers(), getPrincipal(), getAuthorizationToken(), authorizations);
        } else {
            throw new VisalloException("Unhandled element type: " + elementType);
        }
    }

    @Override
    protected void parseArgs(JobConf conf, String[] args) {
        if (args.length != 1) {
            throw new RuntimeException("Required arguments <vertex|edge>");
        }
        elementType = ElementType.valueOf(args[0].toUpperCase());
        LOGGER.info("Element type: " + elementType);
    }

    @Override
    protected String getJobName() {
        return "visalloReindex-" + elementType;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = (AccumuloGraph) graph;
    }
}
