package org.visallo.core.util;

import com.google.common.base.Strings;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.user.User;

public class ModelUtil {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ModelUtil.class);

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
