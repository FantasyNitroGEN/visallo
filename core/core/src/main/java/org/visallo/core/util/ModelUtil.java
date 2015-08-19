package org.visallo.core.util;

import com.google.common.base.Strings;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.vertexium.Graph;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;

public class ModelUtil {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ModelUtil.class);

    public static void drop(
            Graph graph,
            SimpleOrmSession simpleOrmSession,
            WorkQueueRepository workQueueRepository,
            AuthorizationRepository authorizationRepository,
            User user
    ) {
        ModelUtil.deleteTables(simpleOrmSession, user);
        workQueueRepository.format();
        // TODO provide a way to delete the graph and it's search index
        // graph.delete(getUser());

        LOGGER.debug("BEGIN remove all authorizations");
        for (String auth : authorizationRepository.getGraphAuthorizations()) {
            LOGGER.debug("removing auth %s", auth);
            authorizationRepository.removeAuthorizationFromGraph(auth);
        }
        LOGGER.debug("END remove all authorizations");

        graph.drop();
    }

    public static void deleteTables(SimpleOrmSession simpleOrmSession, User user) {
        LOGGER.warn("BEGIN deleting tables");
        String tablePrefix = simpleOrmSession.getTablePrefix();
        if (Strings.isNullOrEmpty(tablePrefix)) {
            throw new VisalloException("Unable to format without a SimpleOrmSession table prefix");
        }
        for (String table : simpleOrmSession.getTableList(user.getSimpleOrmContext())) {
            if (table.startsWith(tablePrefix)) {
                LOGGER.warn("deleting table: %s", table);
                simpleOrmSession.deleteTable(table, user.getSimpleOrmContext());
            }
        }
        LOGGER.warn("END deleting tables");
    }
}
