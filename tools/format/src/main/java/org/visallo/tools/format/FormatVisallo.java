package org.visallo.tools.format;

import com.beust.jcommander.Parameters;
import com.google.inject.Inject;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.core.util.ModelUtil;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.vertexium.GraphConfiguration;
import org.vertexium.elasticsearch.ElasticSearchSearchIndexConfiguration;

import java.util.Map;

@Parameters(commandDescription = "Deletes all tables from Accumulo, indexes from ElasticSearch, queues from RabbitMQ")
public class FormatVisallo extends CommandLineTool {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(FormatVisallo.class);
    private AuthorizationRepository authorizationRepository;
    private SimpleOrmSession simpleOrmSession;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new FormatVisallo(), args);
    }

    @Override
    protected int run() throws Exception {
        ModelUtil.deleteTables(this.simpleOrmSession, getUser());
        getWorkQueueRepository().format();
        // TODO provide a way to delete the graph and it's search index
        // graph.delete(getUser());

        LOGGER.debug("BEGIN remove all authorizations");
        for (String auth : authorizationRepository.getGraphAuthorizations()) {
            LOGGER.debug("removing auth %s", auth);
            authorizationRepository.removeAuthorizationFromGraph(auth);
        }
        LOGGER.debug("END remove all authorizations");

        getGraph().shutdown();

        deleteElasticSearchIndex(getConfiguration().toMap());

        return 0;
    }

    public static void deleteElasticSearchIndex(Map configuration) {
        // TODO refactor to pull graph. from some static reference
        String indexName = (String) configuration.get("graph." + GraphConfiguration.SEARCH_INDEX_PROP_PREFIX + "." + ElasticSearchSearchIndexConfiguration.CONFIG_INDEX_NAME);
        String[] esLocations = ((String) configuration.get("graph." + GraphConfiguration.SEARCH_INDEX_PROP_PREFIX + "." + ElasticSearchSearchIndexConfiguration.CONFIG_ES_LOCATIONS)).split(",");
        LOGGER.debug("BEGIN deleting elastic search index: " + indexName);
        TransportClient client = new TransportClient();
        for (String esLocation : esLocations) {
            String[] locationSocket = esLocation.split(":");
            String host = locationSocket[0];
            String port = locationSocket.length > 1 ? locationSocket[1] : "9300";
            client.addTransportAddress(new InetSocketTransportAddress(host, Integer.parseInt(port)));
        }
        LOGGER.info("index %s exists?", indexName);
        IndicesExistsRequest existsRequest = client.admin().indices().prepareExists(indexName).request();
        if (client.admin().indices().exists(existsRequest).actionGet().isExists()) {
            LOGGER.info("index %s exists... deleting!", indexName);
            DeleteIndexResponse response = client.admin().indices().delete(new DeleteIndexRequest(indexName)).actionGet();
            if (!response.isAcknowledged()) {
                LOGGER.error("Failed to delete elastic search index named %s", indexName);
            }
        }
        LOGGER.debug("END deleting elastic search index: " + indexName);
        client.close();
    }

    @Inject
    public void setSimpleOrmSession(SimpleOrmSession simpleOrmSession) {
        this.simpleOrmSession = simpleOrmSession;
    }

    @Inject
    public void setAuthorizationRepository(AuthorizationRepository authorizationRepository) {
        this.authorizationRepository = authorizationRepository;
    }
}
