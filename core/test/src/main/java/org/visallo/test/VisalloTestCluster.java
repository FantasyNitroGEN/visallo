package org.visallo.test;

import com.google.common.base.Throwables;
import com.v5analytics.simpleorm.AccumuloSimpleOrmContext;
import com.v5analytics.simpleorm.AccumuloSimpleOrmSession;
import com.v5analytics.simpleorm.SimpleOrmContext;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.fate.zookeeper.ZooSession;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.vertexium.GraphBaseWithSearchIndex;
import org.vertexium.GraphConfiguration;
import org.vertexium.elasticsearch.ElasticSearchSearchIndexBase;
import org.vertexium.elasticsearch.ElasticSearchSearchIndexConfiguration;
import org.vertexium.search.SearchIndex;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.bootstrap.VisalloBootstrap;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.config.VisalloTestClusterConfigurationLoader;
import org.visallo.core.ingest.graphProperty.GraphPropertyRunner;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.model.notification.SystemNotificationRepository;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.SystemUser;
import org.visallo.core.util.ModelUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;

public class VisalloTestCluster {
    private static VisalloLogger LOGGER;
    private final int httpPort;
    private final int httpsPort;
    private TestAccumulo accumulo;
    private TestElasticSearch elasticsearch;
    private TestJettyServer jetty;
    private Properties config;
    private WorkQueueNames workQueueNames;
    private GraphPropertyRunner graphPropertyRunner;

    public VisalloTestCluster(int httpPort, int httpsPort) {
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        System.setProperty(ConfigurationLoader.ENV_CONFIGURATION_LOADER, VisalloTestClusterConfigurationLoader.class.getName());
        LOGGER = VisalloLoggerFactory.getLogger(VisalloTestCluster.class);
    }

    public static void main(String[] args) {
        VisalloTestCluster cluster = new VisalloTestCluster(8080, 8443);
        cluster.startup();
    }

    public void startup() {
        try {
            config = VisalloTestClusterConfigurationLoader.getConfigurationProperties();
            Map<String, Object> configMap = new HashMap<>();
            for (Map.Entry<Object, Object> e : config.entrySet()) {
                configMap.put(e.getKey().toString(), e.getValue());
            }
            Configuration configuration = new Configuration(new VisalloTestClusterConfigurationLoader(), configMap);
            workQueueNames = new WorkQueueNames(configuration);
            if (VisalloTestClusterConfigurationLoader.isTestServer()) {
                SimpleOrmSession simpleOrmSession = InjectHelper.getInstance(SimpleOrmSession.class);
                SystemUser user = new SystemUser(simpleOrmSession.createContext(VisalloVisibility.SUPER_USER_VISIBILITY_STRING));
                Graph graph = InjectHelper.getInstance(Graph.class);
                WorkQueueRepository workQueueRepository = InjectHelper.getInstance(WorkQueueRepository.class);
                AuthorizationRepository authorizationRepository = InjectHelper.getInstance(AuthorizationRepository.class);
                ModelUtil.drop(graph, simpleOrmSession, workQueueRepository, authorizationRepository, user);
            } else {
                setupHdfsFiles();
                startAccumulo();
                startElasticSearch();
            }
            startWebServer();
            setupGraphPropertyRunner(configuration);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    shutdown();
                }
            });
        } catch (Exception ex) {
            Throwables.propagate(ex);
        }
    }

    public void setupHdfsFiles() {
        try {
            File hdfsRoot = new File("/tmp/visallo-integration-test");
            File localConfig = new File(getVisalloRootDir(), "./config");
            File hdfsConfig = new File(hdfsRoot, "visallo/config");
            copyFiles(localConfig, hdfsConfig);
        } catch (Exception ex) {
            throw new RuntimeException("Could not setup hdfs files", ex);
        }
    }

    public static File getVisalloRootDir() {
        File startingDir = new File(System.getProperty("user.dir"));
        File f = startingDir;
        while (f != null) {
            if (new File(f, "core").exists() && new File(f, "config").exists()) {
                return f;
            }
            f = f.getParentFile();
        }

        f = new File(startingDir, "visallo-public");
        if (f.exists()) {
            return f;
        }

        throw new RuntimeException("Could not find visallo root starting from " + startingDir.getAbsolutePath());
    }

    private void copyFiles(File sourceDir, File destDir) throws IOException {
        destDir.mkdirs();
        for (File sourceFile : sourceDir.listFiles()) {
            File destFile = new File(destDir, sourceFile.getName());
            if (sourceFile.isDirectory()) {
                copyFiles(sourceFile, destFile);
            } else {
                LOGGER.info("copy file " + sourceFile + " " + destFile);
                FileUtils.copyFile(sourceFile, destFile);
            }
        }
    }

    private void setupGraphPropertyRunner(Configuration configuration) {
        graphPropertyRunner = InjectHelper.getInstance(GraphPropertyRunner.class, VisalloBootstrap.bootstrapModuleMaker(configuration), configuration);
        UserRepository userRepository = InjectHelper.getInstance(UserRepository.class);
        graphPropertyRunner.prepare(userRepository.getSystemUser());
    }

    public void shutdown() {
        try {
            if (jetty != null) {
                jetty.shutdown();
            }

            LOGGER.info("shutdown: graphPropertyRunner");
            if (graphPropertyRunner != null) {
                graphPropertyRunner.shutdown();
            }

            LOGGER.info("shutdown: SimpleOrmSession");
            if (InjectHelper.hasInjector()) {
                SimpleOrmSession simpleOrmSession = InjectHelper.getInstance(SimpleOrmSession.class);
                try {
                    simpleOrmSession.close();
                } catch (IllegalStateException ex) {
                    // ignore this, the model session is already closed.
                }
            }

            LOGGER.info("shutdown: Graph");
            if (InjectHelper.hasInjector()) {
                SystemNotificationRepository systemNotificationRepository = InjectHelper.getInstance(SystemNotificationRepository.class);
                systemNotificationRepository.disable();
            }

            LOGGER.info("shutdown: Graph");
            if (InjectHelper.hasInjector()) {
                Graph graph = InjectHelper.getInstance(Graph.class);
                graph.shutdown();
            }

            Thread.sleep(1000);

            if (!VisalloTestClusterConfigurationLoader.isTestServer()) {
                elasticsearch.shutdown();
                accumulo.shutdown();
                shutdownAndResetZooSession();
            }

            LOGGER.info("shutdown: InjectHelper");
            InjectHelper.shutdown();

            LOGGER.info("shutdown: VisalloBootstrap");
            VisalloBootstrap.shutdown();

            LOGGER.info("shutdown: clear graph property queue");
            getGraphPropertyQueue().clear();

            Thread.sleep(1000);
            LOGGER.info("shutdown complete");
        } catch (InterruptedException e) {
            throw new RuntimeException("failed to sleep", e);
        }
    }

    private void shutdownAndResetZooSession() {
        ZooSession.shutdown();

        try {
            Field sessionsField = ZooSession.class.getDeclaredField("sessions");
            sessionsField.setAccessible(true);
            sessionsField.set(null, new HashMap());
        } catch (Exception ex) {
            throw new RuntimeException("Could not reset ZooSession internal state");
        }
    }

    public Properties getVisalloConfig() {
        return config;
    }

    private void startAccumulo() {
        accumulo = new TestAccumulo(config);
        accumulo.startup();
    }

    private void startElasticSearch() {
        elasticsearch = new TestElasticSearch(config);
        elasticsearch.startup();
    }

    private void startWebServer() {
        File keyStoreFile = new File(getVisalloRootDir(), "core/test/src/main/resources/org/visallo/test/valid.jks");
        File webAppDir = new File(getVisalloRootDir(), "web/war/src/main/webapp");
        jetty = new TestJettyServer(webAppDir, httpPort, httpsPort, keyStoreFile.getAbsolutePath(), "password");
        jetty.startup();
    }

    public List<JSONObject> getGraphPropertyQueue() {
        return InMemoryWorkQueueRepository.getQueue(workQueueNames.getGraphPropertyQueueName());
    }

    public void processGraphPropertyQueue() {
        final List<JSONObject> graphPropertyQueue = getGraphPropertyQueue();
        checkNotNull(graphPropertyQueue, "could not get graphPropertyQueue");

        // need the synchronized inside the loop to give others a chance to change the queue
        while (graphPropertyQueue.size() > 0) {
            JSONObject graphPropertyQueueItem = null;
            synchronized (graphPropertyQueue) {
                if (graphPropertyQueue.size() > 0) {
                    graphPropertyQueueItem = graphPropertyQueue.remove(0);
                }
            }
            if (graphPropertyQueueItem != null) {
                processGraphPropertyQueueItem(graphPropertyQueueItem);
            }
        }
    }

    private void processGraphPropertyQueueItem(JSONObject graphPropertyQueueItem) {
        try {
            LOGGER.info("processGraphPropertyQueueItem: %s", graphPropertyQueueItem.toString(2));
            graphPropertyRunner.process(null, graphPropertyQueueItem);
        } catch (Throwable ex) {
            throw new RuntimeException("graphPropertyRunner process: " + ex.getMessage(), ex);
        }
    }
}
