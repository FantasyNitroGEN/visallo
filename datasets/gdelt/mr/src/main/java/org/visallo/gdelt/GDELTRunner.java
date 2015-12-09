package org.visallo.gdelt;


import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.data.Mutation;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.CounterGroup;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.vertexium.accumulo.AccumuloGraphConfiguration;
import org.vertexium.accumulo.mapreduce.AccumuloElementOutputFormat;
import org.vertexium.accumulo.mapreduce.ElementMapper;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.config.VisalloHadoopConfiguration;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public class GDELTRunner extends Configured implements Tool {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GDELTRunner.class);

    @Override
    public int run(String[] args) throws Exception {
        org.visallo.core.config.Configuration visalloConfig = ConfigurationLoader.load();
        Configuration conf = getConfiguration(args, visalloConfig);
        AccumuloGraphConfiguration accumuloGraphConfiguration = new AccumuloGraphConfiguration(conf, "graph.");
//        InjectHelper.inject(this, VisalloBootstrap.bootstrapModuleMaker(visalloConfig));

        Job job = Job.getInstance(conf, "GDELTImport");

        String instanceName = accumuloGraphConfiguration.getAccumuloInstanceName();
        String zooKeepers = accumuloGraphConfiguration.getZookeeperServers();
        String principal = accumuloGraphConfiguration.getAccumuloUsername();
        AuthenticationToken authorizationToken = accumuloGraphConfiguration.getAuthenticationToken();
        AccumuloElementOutputFormat.setOutputInfo(job, instanceName, zooKeepers, principal, authorizationToken);

        job.setJarByClass(GDELTRunner.class);
        job.setMapperClass(GDELTMapper.class);
        job.setMapOutputValueClass(Mutation.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(AccumuloElementOutputFormat.class);
        FileInputFormat.addInputPath(job, new Path(conf.get("in")));

        int returnCode = job.waitForCompletion(true) ? 0 : 1;

        CounterGroup groupCounters = job.getCounters().getGroup(GDELTImportCounters.class.getName());
        for (Counter counter : groupCounters) {
            System.out.println(counter.getDisplayName() + ": " + counter.getValue());
        }

        return returnCode;
    }

    private Configuration getConfiguration(String[] args, org.visallo.core.config.Configuration visalloConfig) {
        if (args.length < 1) {
            throw new RuntimeException("Required arguments <inputFileName>");
        }
        String inFileName = args[args.length - 1];
        LOGGER.info("Using config:\n" + visalloConfig);

        Configuration hadoopConfig = VisalloHadoopConfiguration.getHadoopConfiguration(visalloConfig);
        hadoopConfig.set(ElementMapper.GRAPH_CONFIG_PREFIX, "graph.");
        LOGGER.info("inFileName: %s", inFileName);
        hadoopConfig.set("in", inFileName);
        this.setConf(hadoopConfig);
        return hadoopConfig;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new GDELTRunner(), args);
        System.exit(res);
    }
}
