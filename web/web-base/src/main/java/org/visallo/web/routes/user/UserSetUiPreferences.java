package org.visallo.web.routes.user;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import org.json.JSONObject;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.BadRequestException;

public class UserSetUiPreferences implements ParameterizedHandler {
    private final UserRepository userRepository;

    @Inject
    public UserSetUiPreferences(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Handle
    public JSONObject handle(
            User user,
            @Optional(name = "ui-preferences") String uiPreferencesString,
            @Optional(name = "name") String propertyName,
            @Optional(name = "value") String propertyValue
    ) throws Exception {
        if (uiPreferencesString != null) {
            userRepository.setUiPreferences(user, new JSONObject(uiPreferencesString));
        } else if (propertyName != null) {
            user.getUiPreferences().put(propertyName, propertyValue);
            userRepository.setUiPreferences(user, user.getUiPreferences());
        } else {
            throw new BadRequestException("ui-preferences", "either ui-preferences or name,value are required parameters.");
        }

        return userRepository.toJsonWithAuths(user);
    }
}
