package org.visallo.themoviedb;

import com.beust.jcommander.Parameter;
import org.visallo.vertexium.mapreduce.VisalloMRBase;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.accumulo.core.data.Mutation;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.vertexium.accumulo.mapreduce.AccumuloElementOutputFormat;

import java.io.File;
import java.util.List;

public class ImportImgMR extends VisalloMRBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ImportImgMR.class);
    private static final String JOB_NAME = "theMovieDbImgImport";

    @Parameter(description = "<infile>")
    private List<String> inFileName;

    @Override
    protected String getJobName() {
        return JOB_NAME;
    }

    @Override
    protected void setupJob(Job job) throws Exception {
        job.setMapperClass(ImportImgMRMapper.class);
        job.setMapOutputValueClass(Mutation.class);
        job.setNumReduceTasks(0);
        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputFormatClass(AccumuloElementOutputFormat.class);

        SequenceFileInputFormat.addInputPath(job, new Path(getConf().get("in")));
    }

    @Override
    protected void processArgs(JobConf conf, String[] args) {
        String inFileName = this.inFileName.get(0);
        conf.set("in", inFileName);
        conf.set(CONFIG_SOURCE_FILE_NAME, new File(inFileName).getName());
        LOGGER.info("inFileName: %s", inFileName);
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new ImportImgMR(), args);
        System.exit(res);
    }
}
