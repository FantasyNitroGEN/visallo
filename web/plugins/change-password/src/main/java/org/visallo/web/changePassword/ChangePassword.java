package org.visallo.web.changePassword;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BadRequestException;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;

public class ChangePassword implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ChangePassword.class);
    private static final String CURRENT_PASSWORD_PARAMETER_NAME = "currentPassword";
    private static final String NEW_PASSWORD_PARAMETER_NAME = "newPassword";
    private static final String NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME = "newPasswordConfirmation";
    private final UserRepository userRepository;

    @Inject
    public ChangePassword(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            User user,
            @Required(name = CURRENT_PASSWORD_PARAMETER_NAME) String currentPassword,
            @Required(name = NEW_PASSWORD_PARAMETER_NAME) String newPassword,
            @Required(name = NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME) String newPasswordConfirmation
    ) throws Exception {
        if (userRepository.isPasswordValid(user, currentPassword)) {
            if (newPassword.length() > 0) {
                if (newPassword.equals(newPasswordConfirmation)) {
                    userRepository.setPassword(user, newPassword);
                    LOGGER.info("changed password for user: %s", user.getUsername());
                    return VisalloResponse.SUCCESS;
                } else {
                    throw new BadRequestException(NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME, "new password and new password confirmation do not match");
                }
            } else {
                throw new BadRequestException(NEW_PASSWORD_PARAMETER_NAME, "new password may not be blank");
            }
        } else {
            LOGGER.warn("failed to change password for user: %s due to incorrect current password", user.getUsername());
            throw new BadRequestException(CURRENT_PASSWORD_PARAMETER_NAME, "incorrect current password");
        }
    }
}
