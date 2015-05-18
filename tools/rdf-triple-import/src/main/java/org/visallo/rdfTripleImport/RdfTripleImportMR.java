package org.visallo.rdfTripleImport;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.vertexium.mapreduce.VisalloMRBase;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.vertexium.accumulo.mapreduce.AccumuloElementOutputFormat;

import java.io.File;
import java.util.List;

public class RdfTripleImportMR extends VisalloMRBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(RdfTripleImportMR.class, "rdfTripleImportMR");
    public static final String CONFIG_VISIBILITY_STRING = "visibility";

    @Parameter(names = {"-v", "--visibility"}, description = "Visibility for new items")
    private String visibilityString;

    @Parameter(description = "Input file name")
    private List<String> inFileName;

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new RdfTripleImportMR(), args);
        System.exit(res);
    }

    @Override
    protected void setupJob(Job job) throws Exception {
        job.getConfiguration().setBoolean("mapred.map.tasks.speculative.execution", false);
        job.getConfiguration().setBoolean("mapred.reduce.tasks.speculative.execution", false);

        job.setJarByClass(RdfTripleImportMR.class);
        job.setMapperClass(RdfTripleImportMapper.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(AccumuloElementOutputFormat.class);
        job.setNumReduceTasks(0);

        TextInputFormat.setInputPaths(job, inFileName.get(0));
    }

    @Override
    protected void parseArgs(JobConf conf, String[] args) {
        new JCommander(this, args);

        if (visibilityString != null) {
            conf.set(CONFIG_VISIBILITY_STRING, new VisalloVisibility(visibilityString).getVisibility().getVisibilityString());
        }
        conf.set("in", inFileName.get(0));
        conf.set(CONFIG_SOURCE_FILE_NAME, new File(inFileName.get(0)).getName());
        LOGGER.info("inFileName: %s", inFileName);
    }

    @Override
    protected String getJobName() {
        return "rdf-triple-import";
    }
}
