package org.visallo.web.changeEmail;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import com.v5analytics.webster.HandlerChain;
import org.visallo.web.BaseRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ChangeEmail extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ChangeEmail.class);
    private static final String EMAIL_PARAMETER_NAME = "email";

    @Inject
    public ChangeEmail(UserRepository userRepository,
                       WorkspaceRepository workspaceRepository,
                       Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        String email = getRequiredParameter(request, EMAIL_PARAMETER_NAME);

        if (user != null) {
            if (email.length() > 0) {
                    getUserRepository().setEmailAddress(user, email);
                    LOGGER.info("changed email for user: %s", user.getUsername());
                    respondWithSuccessJson(response);
            } else {
                respondWithBadRequest(response, EMAIL_PARAMETER_NAME, "new email may not be blank");
            }
        } else {
            LOGGER.error("current user not found while attempting to change email");
            respondWithAccessDenied(response, "current user not found");
        }
    }
}
