package org.visallo.web.routes.longRunningProcess;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import com.v5analytics.webster.HandlerChain;
import org.visallo.web.BaseRequestHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LongRunningProcessDelete extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LongRunningProcessDelete.class);
    private final LongRunningProcessRepository longRunningProcessRepository;

    @Inject
    public LongRunningProcessDelete(
            final WorkspaceRepository workspaceRepo,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration,
            final LongRunningProcessRepository longRunningProcessRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.longRunningProcessRepository = longRunningProcessRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String longRunningProcessId = getRequiredParameter(request, "longRunningProcessId");
        final User authUser = getUser(request);

        LOGGER.info("deleting long running process: %s", longRunningProcessId);
        JSONObject longRunningProcess = longRunningProcessRepository.findById(longRunningProcessId, authUser);
        if (longRunningProcess == null) {
            LOGGER.warn("Could not find long running process: %s", longRunningProcessId);
            respondWithNotFound(response);
        } else {
            LOGGER.debug("Successfully found long running process");
            longRunningProcessRepository.delete(longRunningProcessId, authUser);
            respondWithSuccessJson(response);
        }
    }
}
