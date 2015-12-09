package org.visallo.core.status;

import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.ArrayList;

public class CuratorStatusRepository implements StatusRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(CuratorStatusRepository.class);
    private final CuratorFramework curatorFramework;
    private final String statusPath;

    @Inject
    public CuratorStatusRepository(
            Configuration configuration,
            CuratorFramework curatorFramework
    ) {
        this.curatorFramework = curatorFramework;
        this.statusPath = configuration.get(Configuration.STATUS_ZK_PATH, Configuration.DEFAULT_STATUS_ZK_PATH);
    }

    @Override
    public StatusHandle saveStatus(String group, String instance, StatusData statusData) {
        try {
            String path = getPath(group, instance);
            String zkPath = curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, statusData.toBytes());
            return new CuratorStatusHandle(group, instance, zkPath);
        } catch (Exception e) {
            throw new VisalloException("Could not create ZooKeeper EPHEMERAL node", e);
        }
    }

    @Override
    public void deleteStatus(StatusHandle statusHandle) {
        CuratorStatusHandle csh = (CuratorStatusHandle) statusHandle;
        try {
            curatorFramework.delete().forPath(csh.getZkPath());
        } catch (Throwable ex) {
            LOGGER.error("Could not delete ZK path: %s", csh.getZkPath(), ex);
        }
    }

    @Override
    public Iterable<String> getGroups() {
        try {
            return this.curatorFramework.getChildren().forPath(statusPath);
        } catch (Exception ex) {
            LOGGER.warn("ZooKeeper path '%s' does not exist.", statusPath);
            return new ArrayList<>();
        }
    }

    @Override
    public Iterable<String> getInstances(String group) {
        try {
            return this.curatorFramework.getChildren().forPath(statusPath + "/" + group);
        } catch (Exception e) {
            throw new VisalloException("Could not get instances for group " + group, e);
        }
    }

    @Override
    public StatusData getStatusData(String group, String instance) {
        try {
            byte[] rawData = this.curatorFramework.getData().forPath(statusPath + "/" + group + "/" + instance);
            return new StatusData(rawData);
        } catch (Exception e) {
            throw new VisalloException("Could not get status data for group " + group + " and instance " + instance, e);
        }
    }

    private String getPath(String group, String instance) {
        String path = this.statusPath;
        if (!path.endsWith("/")) {
            path += "/";
        }
        path += group;
        if (!path.endsWith("/")) {
            path += "/";
        }
        path += instance;
        return path;
    }

    private class CuratorStatusHandle extends StatusHandle {
        private final String zkPath;

        public CuratorStatusHandle(String group, String instance, String zkPath) {
            super(group, instance);
            this.zkPath = zkPath;
        }

        public String getZkPath() {
            return zkPath;
        }
    }
}
