package org.visallo.web.changeEmail;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;

public class ChangeEmail implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ChangeEmail.class);
    private static final String EMAIL_PARAMETER_NAME = "email";
    private final UserRepository userRepository;

    @Inject
    public ChangeEmail(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            User user,
            @Required(name = EMAIL_PARAMETER_NAME) String email
    ) throws Exception {
        userRepository.setEmailAddress(user, email);
        LOGGER.info("changed email for user: %s", user.getUsername());
        return VisalloResponse.SUCCESS;
    }
}
