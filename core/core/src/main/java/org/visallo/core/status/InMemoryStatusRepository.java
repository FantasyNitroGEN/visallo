package org.visallo.core.status;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class InMemoryStatusRepository implements StatusRepository {
    private Table<String, String, StatusData> instances = HashBasedTable.create();

    @Override
    public StatusHandle saveStatus(String group, String instance, StatusData statusData) {
        instances.put(group, instance, statusData);
        return new StatusHandle(group, instance);
    }

    @Override
    public void deleteStatus(StatusHandle statusHandle) {
        instances.remove(statusHandle.getGroup(), statusHandle.getInstance());
    }

    @Override
    public Iterable<String> getGroups() {
        return instances.rowKeySet();
    }

    @Override
    public Iterable<String> getInstances(String group) {
        return instances.row(group).keySet();
    }

    @Override
    public StatusData getStatusData(String group, String instance) {
        return instances.get(group, instance);
    }
}
