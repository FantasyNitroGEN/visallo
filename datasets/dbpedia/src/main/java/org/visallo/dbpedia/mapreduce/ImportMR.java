package org.visallo.dbpedia.mapreduce;

import com.beust.jcommander.Parameter;
import org.apache.accumulo.core.data.Mutation;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.vertexium.accumulo.mapreduce.AccumuloElementOutputFormat;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.vertexium.mapreduce.VisalloMRBase;

import java.util.List;

public class ImportMR extends VisalloMRBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ImportMR.class);
    public static final String MULTI_VALUE_KEY = ImportMR.class.getName();

    @Parameter(description = "<infile>")
    private List<String> inFileName;

    @Override
    protected String getJobName() {
        return "dbpediaImport";
    }

    @Override
    protected void setupJob(Job job) throws Exception {
        job.setJarByClass(ImportMR.class);
        job.setMapperClass(ImportMRMapper.class);
        job.setNumReduceTasks(0);
        job.setMapOutputValueClass(Mutation.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(AccumuloElementOutputFormat.class);
        FileInputFormat.addInputPath(job, new Path(getConf().get("in")));
    }

    @Override
    protected void processArgs(JobConf conf, String[] args) {
        String inFileName = this.inFileName.get(0);
        LOGGER.info("inFileName: %s", inFileName);
        conf.set("in", inFileName);
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new ImportMR(), args);
        System.exit(res);
    }
}
