package org.visallo.assignimagemr;

import com.google.inject.Inject;
import org.visallo.core.exception.VisalloException;
import org.visallo.vertexium.mapreduce.VisalloMRBase;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.ToolRunner;
import org.vertexium.Graph;
import org.vertexium.accumulo.AccumuloGraph;
import org.vertexium.accumulo.mapreduce.AccumuloElementOutputFormat;
import org.vertexium.accumulo.mapreduce.AccumuloVertexInputFormat;

public class AssignImageMR extends VisalloMRBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(AssignImageMR.class);
    private AccumuloGraph graph;

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new AssignImageMR(), args);
        System.exit(res);
    }

    @Override
    protected void setupJob(Job job) throws Exception {
        String[] authorizations = new String[]{
                VisalloVisibility.SUPER_USER_VISIBILITY_STRING
        };

        AssignImageConfiguration assignImageConfiguration = new AssignImageConfiguration(job.getConfiguration());
        if (assignImageConfiguration.getHasImageLabels().length == 0) {
            throw new VisalloException("No " + AssignImageConfiguration.HAS_IMAGE_LABELS + " configured.");
        }

        job.getConfiguration().setBoolean("mapred.map.tasks.speculative.execution", false);
        job.getConfiguration().setBoolean("mapred.reduce.tasks.speculative.execution", false);

        job.setJarByClass(AssignImageMR.class);
        job.setMapperClass(AssignImageMRMapper.class);
        job.setOutputFormatClass(AccumuloElementOutputFormat.class);
        job.setNumReduceTasks(0);

        job.setInputFormatClass(AccumuloVertexInputFormat.class);
        AccumuloVertexInputFormat.setInputInfo(job, graph, getInstanceName(), getZooKeepers(), getPrincipal(), getAuthorizationToken(), authorizations);
    }

    @Override
    protected void parseArgs(JobConf conf, String[] args) {
    }

    @Override
    protected String getJobName() {
        return "visalloAssignImage";
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = (AccumuloGraph) graph;
    }
}
