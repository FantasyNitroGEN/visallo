package org.visallo.spark;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.hadoop.io.Text;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkJobInfo;
import org.apache.spark.SparkStageInfo;
import org.apache.spark.api.java.AbstractJavaRDDLike;
import org.apache.spark.api.java.JavaFutureAction;
import org.apache.spark.api.java.JavaRDDLike;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.vertexium.Metadata;
import org.vertexium.Visibility;
import org.vertexium.accumulo.AccumuloGraph;
import org.vertexium.accumulo.KeyValuePair;
import org.vertexium.util.ConfigurationUtils;
import org.vertexium.util.ConvertingIterable;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutionException;

public abstract class VisalloSparkBase implements Serializable {
    private Map configurationMap;
    private Date jobStartDate;
    private boolean dfsClientUseDatanodeHostname;

    @Parameter(names = "-h", description = "Help")
    private boolean help;

    @Parameter(required = true, names = "-fs", description = "Hadoop File System default name (e.g. hdfs://namenode:8020)")
    private String fsDefaultName;

    @Parameter(names = {"-m", "--master"}, description = "Spark master")
    private String master = "local";

    @Parameter(names = "-bs", description = "RFile block size")
    private int blockSize = 128 * 1024 * 1024;

    @Parameter(names = "--numberOfPartitions")
    private int numberOfPartitions = 5;

    protected void run(String[] args) throws ExecutionException, InterruptedException {
        JCommander cmd = new JCommander(this, args);
        if (this.help) {
            cmd.usage();
            throw new RuntimeException("invalid parameters");
        }
        jobStartDate = new Date();

        Set<Class> kryoClasses = getKryoClasses();
        SparkConf conf = new SparkConf()
                .setAppName(this.getClass().getSimpleName())
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .registerKryoClasses(kryoClasses.toArray(new Class[kryoClasses.size()]))
                .setMaster(master);
        JavaSparkContext context = new JavaSparkContext(conf);
        context.setJobGroup(getJobGroupId(), getJobGroupDescription());

        ensureConfigurationLoaded();

        List<NamedJavaFutureAction> futures = createJobs(context);

        printStatus(context, futures);
        for (NamedJavaFutureAction future : futures) {
            future.future.get();
        }
        context.stop();
    }

    protected abstract List<NamedJavaFutureAction> createJobs(JavaSparkContext ctx);

    protected Set<Class> getKryoClasses() {
        Set<Class> results = new HashSet<>();
        results.add(Key.class);
        results.add(Value.class);
        results.add(Text.class);
        results.add(SortByKeyComparator.class);
        results.add(Map.class);
        return results;
    }

    protected void ensureConfigurationLoaded() {
        if (configurationMap == null) {
            configurationMap = new HashMap();

            File configurationDir = new File("/opt/visallo/config");
            Iterable<File> filesList = Lists.newArrayList(configurationDir.listFiles());
            filesList = Iterables.filter(filesList, new Predicate<File>() {
                @Override
                public boolean apply(File input) {
                    return input.isFile();
                }
            });
            Iterable<String> filesIterable = Iterables.transform(filesList, new com.google.common.base.Function<File, String>() {
                public String apply(File f) {
                    return f.getAbsolutePath();
                }
            });
            ArrayList<String> files = Lists.newArrayList(filesIterable);
            try {
                configurationMap = ConfigurationUtils.loadConfig(files, "graph");
                dfsClientUseDatanodeHostname = Boolean.parseBoolean("" + configurationMap.get("dfs.client.use.datanode.hostname"));
            } catch (IOException e) {
                throw new RuntimeException("Could not load configuration files", e);
            }
        }
    }

    protected abstract String getJobGroupId();

    protected abstract String getJobGroupDescription();

    private void printStatus(
            final JavaSparkContext context,
            final List<NamedJavaFutureAction> futures
    ) throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!NamedJavaFutureAction.isDone(futures)) {
                    System.out.println("Status: " + new Date());
                    for (NamedJavaFutureAction future : futures) {
                        getFutureStatus(future.name, context, future.future);
                    }
                    System.out.println("--------------------------------------------------------------------");
                    try {
                        Thread.sleep(60 * 1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Could not sleep", e);
                    }
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void getFutureStatus(String futureName, JavaSparkContext context, JavaFutureAction<Void> future) {
        List<Integer> jobIds = future.jobIds();
        if (jobIds.isEmpty()) {
            System.out.println(String.format("%20s: job ids empty", futureName));
            return;
        }
        int currentJobId = jobIds.get(jobIds.size() - 1);
        SparkJobInfo jobInfo = context.statusTracker().getJobInfo(currentJobId);
        if (jobInfo == null) {
            System.out.println(String.format("%20s: Could not get job info for job id '%d'", futureName, currentJobId));
            return;
        }
        int[] stageIds = jobInfo.stageIds();
        if (stageIds == null) {
            System.out.println(String.format("%20s: Could not get stage ids for job id '%d'", futureName, currentJobId));
            return;
        }
        SparkStageInfo stageInfo = context.statusTracker().getStageInfo(stageIds[0]);
        System.out.println(String.format("%20s: %5d tasks total: %5d active, %5d complete", futureName, stageInfo.numTasks(), stageInfo.numActiveTasks(), stageInfo.numCompletedTasks()));
    }

    protected <T, This extends JavaRDDLike<T, This>> JavaFutureAction<Void> convertToAccumuloRFileOutput(
            String outputPath,
            AbstractJavaRDDLike<T, This> rdd,
            PairFlatMapFunction<T, Key, Value> mapFunction
    ) {
        return rdd.flatMapToPair(mapFunction)
                .repartitionAndSortWithinPartitions(new KeyPartitioner(numberOfPartitions), new SortByKeyComparator())
                .foreachPartitionAsync(new AccumuloRFileOutput(outputPath, fsDefaultName, blockSize, jobStartDate, dfsClientUseDatanodeHostname));
    }

    protected AccumuloGraph createGraph() {
        try {
            ensureConfigurationLoaded();
            return AccumuloGraph.create(configurationMap);
        } catch (Exception e) {
            throw new RuntimeException("Could not create graph", e);
        }
    }

    protected abstract class FlatMapBase<T> implements PairFlatMapFunction<T, Key, Value> {
        @Override
        public final Iterable<Tuple2<Key, Value>> call(T obj) throws Exception {
            ensureInitialized();
            long timestamp = System.currentTimeMillis();
            Metadata metadata = createMetadata(obj);
            Iterable<KeyValuePair> keyValuePairs = callInternal(obj, metadata, timestamp);
            return new ConvertingIterable<KeyValuePair, Tuple2<Key, Value>>(keyValuePairs) {
                @Override
                protected Tuple2<Key, Value> convert(KeyValuePair o) {
                    return new Tuple2<>(o.getKey(), o.getValue());
                }
            };
        }

        private Metadata createMetadata(T obj) {
            Date modifiedDate = new Date();
            Metadata metadata = new Metadata();
            Visibility visibility = getMetadataVisibility(obj);
            metadata.add("http://visallo.org#confidence", 0.5, visibility);
            metadata.add("http://visallo.org#modifiedBy", "USER_system", visibility);
            metadata.add("http://visallo.org#modifiedDate", modifiedDate, visibility);
            metadata.add("http://visallo.org#visibilityJson", "{\"source\":\"\"}", visibility);
            return metadata;
        }

        protected abstract Visibility getMetadataVisibility(T obj);

        protected transient AccumuloGraph graph;

        private void ensureInitialized() {
            if (graph == null) {
                graph = createGraph();
            }
        }

        protected abstract Iterable<KeyValuePair> callInternal(T obj, Metadata metadata, long timestamp) throws Exception;
    }
}
