package org.visallo.core.model.hazelcast.store;

import org.apache.commons.dbcp2.BasicDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.Update;
import org.skife.jdbi.v2.exceptions.DBIException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.hazelcast.HazelcastConfiguration;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.*;

public abstract class SqlHazelcastStoreBase<TKey, TValue> {
    private final String selectKeysSql;
    private final String insertSql;
    private final String updateByKeySql;
    private final String deleteByKeySql;
    private final String selectByKeySql;
    private final DBI dbi;

    public SqlHazelcastStoreBase(String name, StoreType type, HazelcastConfiguration hazelcastConfiguration) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(hazelcastConfiguration.getJdbcDriverClass());
        dataSource.setUrl(hazelcastConfiguration.getJdbcConnectionString());
        dataSource.setUsername(hazelcastConfiguration.getJdbcUserName());
        dataSource.setPassword(hazelcastConfiguration.getJdbcPassword());
        dataSource.setDefaultAutoCommit(true);
        dbi = new DBI(dataSource);

        try (Handle handle = dbi.open()) {
            handle.execute(hazelcastConfiguration.getJdbcCreateSql(name, type));
        }
        this.selectKeysSql = hazelcastConfiguration.getJdbcSelectKeysSql(name, type);
        this.insertSql = hazelcastConfiguration.getJdbcInsertSql(name, type);
        this.updateByKeySql = hazelcastConfiguration.getJdbcUpdateByKeySql(name, type);
        this.deleteByKeySql = hazelcastConfiguration.getJdbcDeleteByKeySql(name, type);
        this.selectByKeySql = hazelcastConfiguration.getJdbcSelectByKeySql(name, type);
    }

    protected void store(TKey key, TValue value) {
        try (Handle handle = dbi.open()) {
            try {
                Update stmt = handle.createStatement(insertSql)
                        .bind(HazelcastConfiguration.SQL_KEY_COLUMN, key);
                setDataInPreparedStatement(stmt, value);
                if (stmt.execute() == 0) {
                    throw new SQLException("Failed to insert item expected more than 0 rows effected. Attempting to update instead.");
                }
            } catch (SQLException | DBIException ex) {
                Update stmt = handle.createStatement(updateByKeySql)
                        .bind(HazelcastConfiguration.SQL_KEY_COLUMN, key);
                setDataInPreparedStatement(stmt, value);
                stmt.execute();
            }
        }
    }

    protected void delete(TKey key) {
        try (Handle handle = dbi.open()) {
            handle.createStatement(deleteByKeySql)
                    .bind(HazelcastConfiguration.SQL_KEY_COLUMN, key)
                    .execute();
        }
    }

    protected TValue load(TKey key) {
        try (Handle handle = dbi.open()) {
            Map<String, Object> first = handle.createQuery(selectByKeySql)
                    .bind(HazelcastConfiguration.SQL_KEY_COLUMN, key)
                    .first();
            if (first == null) {
                return null;
            }
            return getDataFromQueryRow(first);
        }
    }

    protected Iterable<TKey> loadAllKeysIterable() {
        try (Handle handle = dbi.open()) {
            Query<Map<String, Object>> query = handle.createQuery(selectKeysSql);
            List<TKey> results = new ArrayList<>();
            for (Map<String, Object> row : query) {
                results.add(getKeyFromQueryRow(row));
            }
            return results;
        }
    }

    protected Map<TKey, TValue> loadAll(Collection<TKey> keys) {
        Map<TKey, TValue> results = new HashMap<>();
        for (TKey key : keys) {
            TValue data = load(key);
            if (data != null) {
                results.put(key, data);
            }
        }
        return results;
    }

    protected void deleteAll(Collection<TKey> keys) {
        for (TKey key : keys) {
            delete(key);
        }
    }

    protected void storeAll(Map<TKey, TValue> map) {
        for (Map.Entry<TKey, TValue> entry : map.entrySet()) {
            store(entry.getKey(), entry.getValue());
        }
    }

    protected abstract TKey getKeyFromQueryRow(Map<String, Object> row);

    protected TValue getDataFromQueryRow(Map<String, Object> row) {
        byte[] bytes;
        try {
            Object columnValue = getDataObjectFromQueryRow(row);
            if (columnValue instanceof Blob) {
                Blob blob = (Blob) columnValue;
                bytes = blob.getBytes(0, (int) blob.length());
            } else if (columnValue instanceof byte[]) {
                bytes = (byte[]) columnValue;
            } else {
                throw new VisalloException("Unsupported result object: " + columnValue);
            }
        } catch (Throwable ex) {
            throw new VisalloException("Could not get data from column", ex);
        }
        return deserializeValue(bytes);
    }

    protected abstract TValue deserializeValue(byte[] bytes);

    protected Object getKeyObjectFromQueryRow(Map<String, Object> row) {
        return row.get(HazelcastConfiguration.SQL_KEY_COLUMN);
    }

    protected Object getDataObjectFromQueryRow(Map<String, Object> row) {
        return row.get(HazelcastConfiguration.SQL_DATA_COLUMN);
    }

    protected void setDataInPreparedStatement(Update stmt, TValue value) {
        stmt.bind(HazelcastConfiguration.SQL_DATA_COLUMN, value);
    }
}
