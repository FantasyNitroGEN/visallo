package org.visallo.core.config;

import org.visallo.test.VisalloTestCluster;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class VisalloTestClusterConfigurationLoader extends ConfigurationLoader {
    private static Properties props;
    private static boolean usingTestServer = false;

    static {
        loadProps();
    }

    public VisalloTestClusterConfigurationLoader() {
        this(new HashMap());
    }

    public VisalloTestClusterConfigurationLoader(Map initParameters) {
        super(initParameters);
    }

    @Override
    public Configuration createConfiguration() {
        return new Configuration(this, props);
    }

    @Override
    public File resolveFileName(String fileName) {
        File resourceDir = new File(VisalloTestCluster.getVisalloRootDir(), "core/test/src/main/resources/org/visallo/test");
        return new File(resourceDir, fileName);
    }

    public static Properties getConfigurationProperties() {
        return props;
    }

    public static void set(String key, String value) {
        props.put(key, value);
    }

    public static boolean isTestServer() {
        return usingTestServer;
    }

    private static void loadProps() {
        try {
            props = new Properties();
            props.load(VisalloTestClusterConfigurationLoader.class.getResourceAsStream("/org/visallo/test/visallo.properties"));

            String repositoryOntology = System.getProperty("repository.ontology");
            if (repositoryOntology != null && repositoryOntology.length() > 0) {
                props.setProperty("repository.ontology", repositoryOntology);
            }

            String testServer = System.getProperty("testServer");
            if (testServer != null && testServer.length() > 0) {
                usingTestServer = true;
                props.setProperty("hadoop.url", "hdfs://" + testServer + ":8020");
                props.setProperty("zookeeper.serverNames", testServer);

                props.setProperty("simpleOrm.accumulo.instanceName", "visallo");
                props.setProperty("simpleOrm.accumulo.zookeeperServerNames", testServer);
                props.setProperty("simpleOrm.accumulo.username", "root");
                props.setProperty("simpleOrm.accumulo.password", "password");

                props.setProperty("graph.accumuloInstanceName", "visallo");
                props.setProperty("graph.username", "root");
                props.setProperty("graph.password", "password");
                props.setProperty("graph.tableNamePrefix", "visallo_vertexium");
                props.setProperty("graph.zookeeperServers", testServer);
                props.setProperty("graph.search.locations", testServer);
                props.setProperty("graph.search.indexName", "vertexium");
                props.setProperty("graph.hdfs.rootDir", "hdfs://" + testServer);

                props.setProperty("objectdetection.classifier.face.path", props.getProperty("objectdetection.classifier.face.path").replace("/tmp/visallo-integration-test", ""));
                props.setProperty("termextraction.opennlp.pathPrefix", props.getProperty("termextraction.opennlp.pathPrefix").replace("file:///tmp/visallo-integration-test", ""));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
