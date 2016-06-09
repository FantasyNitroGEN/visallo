package org.visallo.core.util;

import com.google.common.base.Strings;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.vertexium.Graph;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.GraphAuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;

public class ModelUtil {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ModelUtil.class);

    public static void drop(
            Graph graph,
            SimpleOrmSession simpleOrmSession,
            UserRepository userRepository,
            WorkQueueRepository workQueueRepository,
            GraphAuthorizationRepository graphAuthorizationRepository,
            User user
    ) {
        ModelUtil.clearTables(userRepository, simpleOrmSession, user);
        workQueueRepository.format();
        // TODO provide a way to delete the graph and it's search index
        // graph.delete(getUser());

        LOGGER.debug("BEGIN remove all authorizations");
        for (String auth : graphAuthorizationRepository.getGraphAuthorizations()) {
            LOGGER.debug("removing auth %s", auth);
            graphAuthorizationRepository.removeAuthorizationFromGraph(auth);
        }
        LOGGER.debug("END remove all authorizations");

        graph.drop();
    }

    public static void clearTables(UserRepository userRepository, SimpleOrmSession simpleOrmSession, User user) {
        LOGGER.warn("BEGIN clearing tables");
        String tablePrefix = simpleOrmSession.getTablePrefix();
        if (Strings.isNullOrEmpty(tablePrefix)) {
            throw new VisalloException("Unable to format without a SimpleOrmSession table prefix");
        }
        for (String table : simpleOrmSession.getTableList(userRepository.getSimpleOrmContext(user))) {
            if (table.startsWith(tablePrefix)) {
                LOGGER.warn("clearing table: %s", table);
                simpleOrmSession.clearTable(table, userRepository.getSimpleOrmContext(user));
            }
        }
        LOGGER.warn("END clearing tables");
    }
}
