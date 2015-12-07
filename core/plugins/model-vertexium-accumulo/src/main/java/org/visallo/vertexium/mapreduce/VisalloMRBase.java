package org.visallo.vertexium.mapreduce;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.CounterGroup;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobStatus;
import org.apache.hadoop.util.Tool;
import org.vertexium.accumulo.AccumuloGraphConfiguration;
import org.vertexium.accumulo.mapreduce.AccumuloElementOutputFormat;
import org.vertexium.accumulo.mapreduce.ElementMapper;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.bootstrap.VisalloBootstrap;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.config.VisalloHadoopConfiguration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VersionUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public abstract class VisalloMRBase extends Configured implements Tool {
    private static VisalloLogger LOGGER;
    public static final String CONFIG_SOURCE_FILE_NAME = "sourceFileName";
    public static final int PERIODIC_COUNTER_OUTPUT_PERIOD = 30 * 1000;
    private String instanceName;
    private String zooKeepers;
    private String principal;
    private AuthenticationToken authorizationToken;
    private boolean local;
    private Timer periodicCounterOutputTimer;
    private org.visallo.core.config.Configuration visalloConfig;
    private AccumuloGraphConfiguration accumuloGraphConfiguration;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @DynamicParameter(names = {"-job"}, description = "Set a job property. (e.g.: -job mapreduce.map.memory.mb=1024)")
    private Map<String, String> jobProperties = new HashMap<>();

    @Parameter(names = {"--help", "-h"}, description = "Print help", help = true)
    private boolean help;

    @Parameter(names = {"--version"}, description = "Print version")
    private boolean version;

    @Override
    public int run(String[] args) throws Exception {
        LOGGER = VisalloLoggerFactory.getLogger(VisalloMRBase.class);

        visalloConfig = ConfigurationLoader.load();
        JobConf conf = getConfiguration(args, visalloConfig);
        if (conf == null) {
            return -1;
        }
        accumuloGraphConfiguration = new AccumuloGraphConfiguration(conf, "graph.");
        InjectHelper.inject(this, VisalloBootstrap.bootstrapModuleMaker(visalloConfig), visalloConfig);

        Job job = Job.getInstance(conf, getJobName());

        instanceName = accumuloGraphConfiguration.getAccumuloInstanceName();
        zooKeepers = accumuloGraphConfiguration.getZookeeperServers();
        principal = accumuloGraphConfiguration.getAccumuloUsername();
        authorizationToken = accumuloGraphConfiguration.getAuthenticationToken();
        AccumuloElementOutputFormat.setOutputInfo(job, instanceName, zooKeepers, principal, authorizationToken);

        boolean periodicCounterOutput = conf.getBoolean("visallo.periodic.counter.output.enabled", false);

        if (job.getConfiguration().get("mapred.job.tracker").equals("local")) {
            LOGGER.warn("!!!!!! Running in local mode !!!!!!");
            local = true;
            periodicCounterOutput = true;
        }

        setupJob(job);

        if (periodicCounterOutput) {
            startPeriodicCounterOutputThread(job);
        }

        LOGGER.info("Starting job");
        long startTime = System.currentTimeMillis();
        int result = job.waitForCompletion(true) ? 0 : 1;
        long endTime = System.currentTimeMillis();
        LOGGER.info("Job complete");

        if (job.getStatus().getState() != JobStatus.State.SUCCEEDED) {
            LOGGER.warn("Unexpected job state: %s", job.getStatus().getState());
        }

        if (periodicCounterOutputTimer != null) {
            periodicCounterOutputTimer.cancel();
        }

        printCounters(job);
        LOGGER.info("Time: %,.2f minutes", ((double) (endTime - startTime) / 1000.0 / 60.0));
        LOGGER.info("Return code: " + result);

        return result;
    }

    public boolean isLocal() {
        return local;
    }

    protected void printCounters(Job job) {
        try {
            if (job.getJobState() != JobStatus.State.RUNNING) {
                return;
            }
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("Job in state DEFINE instead of RUNNING")) {
                return;
            }
            throw new VisalloException("Could not get job state", e);
        } catch (Exception e) {
            throw new VisalloException("Could not get job state", e);
        }
        try {
            LOGGER.info("Counters");
            for (String groupName : job.getCounters().getGroupNames()) {
                CounterGroup groupCounters = job.getCounters().getGroup(groupName);
                LOGGER.info(groupCounters.getDisplayName());
                for (Counter counter : groupCounters) {
                    LOGGER.info("  " + counter.getDisplayName() + ": " + counter.getValue());
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Could not print counters", ex);
        }
    }

    protected String getJobName() {
        return this.getClass().getSimpleName();
    }

    protected abstract void setupJob(Job job) throws Exception;

    protected JobConf getConfiguration(String[] args, org.visallo.core.config.Configuration visalloConfig) {
        Configuration hadoopConfig = VisalloHadoopConfiguration.toHadoopConfiguration(visalloConfig, getConf());
        hadoopConfig.set(ElementMapper.GRAPH_CONFIG_PREFIX, "graph.");
        JobConf result = new JobConf(hadoopConfig, this.getClass());
        JCommander j = new JCommander(this, args);
        j.setProgramName("hadoop jar <jar>");
        if (help) {
            j.usage();
            return null;
        }
        if (version) {
            VersionUtil.printVersion();
            return null;
        }
        processArgs(result, args);
        for (Map.Entry<String, String> jobProperty : jobProperties.entrySet()) {
            result.set(jobProperty.getKey(), jobProperty.getValue());
            LOGGER.info("setting config: %s = %s", jobProperty.getKey(), jobProperty.getValue());
        }
        setConf(result);
        LOGGER.info("Using config:\n" + result);
        return result;
    }

    protected abstract void processArgs(JobConf conf, String[] args);

    public String getInstanceName() {
        return instanceName;
    }

    public String getZooKeepers() {
        return zooKeepers;
    }

    public String getPrincipal() {
        return principal;
    }

    public org.visallo.core.config.Configuration getVisalloConfig() {
        return visalloConfig;
    }

    public AccumuloGraphConfiguration getAccumuloGraphConfiguration() {
        return accumuloGraphConfiguration;
    }

    public AuthenticationToken getAuthorizationToken() {
        return authorizationToken;
    }

    private void startPeriodicCounterOutputThread(final Job job) {
        periodicCounterOutputTimer = new Timer("periodicCounterOutput", true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                printCounters(job);
            }
        };
        periodicCounterOutputTimer.scheduleAtFixedRate(task, PERIODIC_COUNTER_OUTPUT_PERIOD, PERIODIC_COUNTER_OUTPUT_PERIOD);
    }
}
