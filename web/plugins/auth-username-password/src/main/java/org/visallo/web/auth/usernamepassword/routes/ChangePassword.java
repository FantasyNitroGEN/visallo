package org.visallo.web.auth.usernamepassword.routes;

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

import java.util.Date;

public class ChangePassword implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ChangePassword.class);
    public static final String TOKEN_PARAMETER_NAME = "token";
    public static final String NEW_PASSWORD_PARAMETER_NAME = "newPassword";
    public static final String NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME = "newPasswordConfirmation";
    private final UserRepository userRepository;

    @Inject
    public ChangePassword(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            @Required(name = TOKEN_PARAMETER_NAME) String token,
            @Required(name = NEW_PASSWORD_PARAMETER_NAME) String newPassword,
            @Required(name = NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME) String newPasswordConfirmation
    ) throws Exception {
        User user = userRepository.findByPasswordResetToken(token);
        if (user == null) {
            throw new BadRequestException("invalid token");
        }

        Date now = new Date();
        if (!user.getPasswordResetTokenExpirationDate().after(now)) {
            throw new BadRequestException("expired token");
        }

        if (newPassword.length() <= 0) {
            throw new BadRequestException(NEW_PASSWORD_PARAMETER_NAME, "new password may not be blank");
        }

        if (!newPassword.equals(newPasswordConfirmation)) {
            throw new BadRequestException(NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME, "new password and new password confirmation do not match");
        }

        userRepository.setPassword(user, newPassword);
        userRepository.clearPasswordResetTokenAndExpirationDate(user);
        LOGGER.info("changed password for user: %s", user.getUsername());

        return VisalloResponse.SUCCESS;
    }
}
