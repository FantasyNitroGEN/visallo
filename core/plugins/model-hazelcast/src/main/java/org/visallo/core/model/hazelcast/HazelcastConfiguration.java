package org.visallo.core.model.hazelcast;

import com.hazelcast.config.*;
import com.hazelcast.nio.serialization.Serializer;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.visallo.core.config.Configurable;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.hazelcast.store.SqlHazelcastStoreFactory;
import org.visallo.core.model.hazelcast.store.StoreType;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.IOException;

public class HazelcastConfiguration {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(HazelcastConfiguration.class);
    public static final String INSTANCE_NAME_DEFAULT = "visallo";
    public static final String BROADCAST_TOPIC_NAME_DEFAULT = "visallo.broadcast";
    public static final String USER_SESSION_COUNTER_MAP_NAME_DEFAULT = "visallo.userCounterMap";
    public static final String STATUS_MAP_NAME_DEFAULT = "visallo.statusMap";
    public static final String CONFIGURATION_PREFIX = "hazelcast";
    public static final String SQL_TABLE_NAME_PREFIX_DEFAULT = "visallo_hazelcast_";
    public static final String SQL_KEY_COLUMN = "id";
    public static final String SQL_DATA_COLUMN = "data";
    private Config config;

    private HazelcastConfiguration() {

    }

    @Configurable(defaultValue = INSTANCE_NAME_DEFAULT)
    private String instanceName;

    @Configurable(defaultValue = BROADCAST_TOPIC_NAME_DEFAULT)
    private String broadcastTopicName;

    @Configurable(defaultValue = USER_SESSION_COUNTER_MAP_NAME_DEFAULT)
    private String userSessionCounterMapName;

    @Configurable(defaultValue = STATUS_MAP_NAME_DEFAULT)
    private String statusMapName;

    @Configurable
    private String configFilePath;

    @Configurable(name = "jdbc.driverClass", required = true)
    private String jdbcDriverClass;

    @Configurable(name = "jdbc.connectionString", required = true)
    private String jdbcConnectionString;

    @Configurable(name = "jdbc.userName", required = true)
    private String jdbcUserName;

    @Configurable(name = "jdbc.password", required = true)
    private String jdbcPassword;

    @Configurable(name = "jdbc.tableNamePrefix", defaultValue = SQL_TABLE_NAME_PREFIX_DEFAULT)
    private String jdbcTableNamePrefix;

    public String getInstanceName() {
        return instanceName;
    }

    public String getBroadcastTopicName() {
        return broadcastTopicName;
    }

    public String getUserSessionCounterMapName() {
        return userSessionCounterMapName;
    }

    public String getStatusMapName() {
        return statusMapName;
    }

    public String getConfigFilePath() {
        return configFilePath;
    }

    public String getJdbcDriverClass() {
        return jdbcDriverClass;
    }

    public String getJdbcConnectionString() {
        return jdbcConnectionString;
    }

    public String getJdbcUserName() {
        return jdbcUserName;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public String getJdbcTableNamePrefix() {
        return jdbcTableNamePrefix;
    }

    public Config getConfig() {
        if (config == null) {
            config = createConfig();
        }
        return config;
    }

    private Config createConfig() {
        Config config = loadConfig();
        updateInstanceName(config);
        updateQueueConfig(config);
        updateMapConfig(config);
        registerSerializer(config, JSONObject.class, HazelcastJSONObjectSerializer.class);
        return config;
    }

    private Config loadConfig() {
        Config config;
        String configFilePath = getConfigFilePath();
        if (StringUtils.isEmpty(configFilePath)) {
            LOGGER.info("hazelcast.configFilePath not set using default configuration");
            config = new Config();
        } else {
            try {
                LOGGER.info("loading hazelcast configuration: %s", configFilePath);
                config = ConfigLoader.load(configFilePath);
            } catch (IOException ex) {
                throw new VisalloException("Could not load Hazelcast config: " + configFilePath, ex);
            }
        }
        return config;
    }

    private void updateInstanceName(Config config) {
        if (StringUtils.isEmpty(config.getInstanceName())) {
            config.setInstanceName(getInstanceName());
        }
    }

    private void updateMapConfig(Config config) {
        MapConfig mapConfig = config.getMapConfig("default");
        if (mapConfig == null) {
            mapConfig = new MapConfig("default");
            mapConfig.setMapStoreConfig(new MapStoreConfig());
            config.addMapConfig(mapConfig);
        }
        MapStoreConfig mapStoreConfig = mapConfig.getMapStoreConfig();
        if (mapStoreConfig == null) {
            mapStoreConfig = new MapStoreConfig();
            mapConfig.setMapStoreConfig(mapStoreConfig);
        }
        mapStoreConfig.setEnabled(true);
        mapStoreConfig.setProperty("binary", "true");
        mapStoreConfig.setFactoryImplementation(new SqlHazelcastStoreFactory(this));
    }

    private void updateQueueConfig(Config config) {
        QueueConfig queueConfig = config.getQueueConfig("default");
        if (queueConfig == null) {
            queueConfig = new QueueConfig("default");
            config.addQueueConfig(queueConfig);
        }
        QueueStoreConfig queueStoreConfig = queueConfig.getQueueStoreConfig();
        if (queueStoreConfig == null) {
            queueStoreConfig = new QueueStoreConfig();
            queueConfig.setQueueStoreConfig(queueStoreConfig);
        }
        queueStoreConfig.setEnabled(true);
        queueStoreConfig.setProperty("binary", "true");
        queueStoreConfig.setFactoryImplementation(new SqlHazelcastStoreFactory(this));
    }

    private void registerSerializer(Config config, Class<?> typeClass, Class<? extends Serializer> serializerClass) {
        SerializerConfig jsonObjectSerializerConfig = new SerializerConfig();
        jsonObjectSerializerConfig.setTypeClass(typeClass);
        jsonObjectSerializerConfig.setClass(serializerClass);
        config.getSerializationConfig().addSerializerConfig(jsonObjectSerializerConfig);
    }

    public static HazelcastConfiguration create(Configuration configuration) {
        HazelcastConfiguration hazelcastConfiguration = new HazelcastConfiguration();
        configuration.setConfigurables(hazelcastConfiguration, CONFIGURATION_PREFIX);
        return hazelcastConfiguration;
    }

    public String getJdbcCreateSql(String name, StoreType type) {
        String sqlType;
        switch (type) {
            case MAP:
                sqlType = "VARCHAR(1000)";
                break;
            case QUEUE:
                sqlType = "INTEGER";
                break;
            default:
                throw new VisalloException("Unhandled " + StoreType.class.getName() + ":" + type);
        }
        String sql = String.format("CREATE TABLE IF NOT EXISTS %%tableName%% (" +
                "id %s PRIMARY KEY," +
                "data LONGBLOB" +
                ")", sqlType);
        return performSqlReplacements(sql, name, type);
    }

    public String getJdbcSelectKeysSql(String name, StoreType type) {
        return performSqlReplacements("SELECT id FROM %tableName%", name, type);
    }

    public String getJdbcInsertSql(String name, StoreType type) {
        return performSqlReplacements("INSERT INTO %tableName% (id,data) VALUES (:id,:data)", name, type);
    }

    public String getJdbcUpdateByKeySql(String name, StoreType type) {
        return performSqlReplacements("UPDATE %tableName% SET data=:data WHERE id=:id", name, type);
    }

    public String getJdbcDeleteByKeySql(String name, StoreType type) {
        return performSqlReplacements("DELETE FROM %tableName% WHERE id=:id", name, type);
    }

    public String getJdbcSelectByKeySql(String name, StoreType type) {
        return performSqlReplacements("SELECT id,data FROM %tableName% WHERE id=:id", name, type);
    }

    private String performSqlReplacements(String sql, String name, StoreType type) {
        String tableName = getTableName(name, type);
        return sql.replace("%tableName%", tableName);
    }

    private String getTableName(String name, StoreType type) {
        name = name.replace('.', '_').toLowerCase();
        return getJdbcTableNamePrefix() + type.name().toLowerCase() + "_" + name;
    }
}
