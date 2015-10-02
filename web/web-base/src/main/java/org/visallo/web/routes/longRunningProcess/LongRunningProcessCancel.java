package org.visallo.web.routes.longRunningProcess;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONObject;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;

public class LongRunningProcessCancel implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LongRunningProcessCancel.class);
    private final LongRunningProcessRepository longRunningProcessRepository;

    @Inject
    public LongRunningProcessCancel(final LongRunningProcessRepository longRunningProcessRepository) {
        this.longRunningProcessRepository = longRunningProcessRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            User authUser,
            @Required(name = "longRunningProcessId") String longRunningProcessId
    ) throws Exception {
        LOGGER.info("Attempting to cancel long running process: %s", longRunningProcessId);
        JSONObject longRunningProcess = longRunningProcessRepository.findById(longRunningProcessId, authUser);
        if (longRunningProcess == null) {
            throw new VisalloResourceNotFoundException("Could not find long running process: %s", longRunningProcessId);
        } else {
            longRunningProcessRepository.cancel(longRunningProcessId, authUser);
            return VisalloResponse.SUCCESS;
        }
    }
}
