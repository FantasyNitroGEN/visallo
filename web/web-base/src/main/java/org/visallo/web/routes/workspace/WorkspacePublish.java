package org.visallo.web.routes.workspace;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.vertexium.Authorizations;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiPublishItem;
import org.visallo.web.clientapi.model.ClientApiWorkspacePublishResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorkspacePublish extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspacePublish.class);

    @Inject
    public WorkspacePublish(
            final UserRepository userRepository,
            final Configuration configuration,
            final WorkspaceRepository workspaceRepository
    ) {
        super(userRepository, workspaceRepository, configuration);

    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String publishDataString = getRequiredParameter(request, "publishData");
        ClientApiPublishItem[] publishData = getObjectMapper().readValue(publishDataString, ClientApiPublishItem[].class);
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);

        LOGGER.debug("publishing:\n%s", Joiner.on("\n").join(publishData));
        ClientApiWorkspacePublishResponse workspacePublishResponse = getWorkspaceRepository().publish(publishData, workspaceId, user, authorizations);

        LOGGER.debug("publishing results: %s", workspacePublishResponse);
        respondWithClientApiObject(response, workspacePublishResponse);
    }
}
