package org.visallo.web.session;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.CuratorUserSessionCounterRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

/**
 * This implementation adds periodic session cleanup to the Apache Curator-based base class.
 */
@Singleton
public class WebCuratorUserSessionCounterRepository extends CuratorUserSessionCounterRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WebCuratorUserSessionCounterRepository.class);
    private static final String LEADER_SEGMENT = "/leader";

    @Inject
    public WebCuratorUserSessionCounterRepository(CuratorFramework curatorFramework, Configuration configuration) {
        super(curatorFramework, configuration);
        String leaderPath = basePath + LEADER_SEGMENT;
        try {
            tryCreate(leaderPath);
        } catch (Exception e) {
            throw new VisalloException("unable to create base path " + basePath, e);
        }
        LeaderSelector leaderSelector = new LeaderSelector(curator, leaderPath,
                new LeaderSelectorListenerAdapter() {
                    @Override
                    public void takeLeadership(CuratorFramework client) throws Exception {
                        try {
                            LOGGER.info("starting user session cleanup");
                            while (true) {
                                try {
                                    deleteOldSessions();
                                } catch (Exception e) {
                                    LOGGER.error("failed to delete old sessions", e);
                                }
                                Thread.sleep(SESSION_UPDATE_DURATION);
                            }
                        } catch (InterruptedException e) {
                            LOGGER.info("stopping user session cleanup");
                            throw e;
                        }
                    }
                });
        leaderSelector.autoRequeue();
        leaderSelector.start();
    }
}
