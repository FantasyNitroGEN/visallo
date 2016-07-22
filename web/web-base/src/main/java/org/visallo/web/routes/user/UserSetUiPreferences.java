package org.visallo.web.routes.user;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONObject;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;

public class UserSetUiPreferences implements ParameterizedHandler {
    private final UserRepository userRepository;

    @Inject
    public UserSetUiPreferences(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            User user,
            @Required(name = "name") String propertyName,
            @Required(name = "value") String propertyValue
    ) throws Exception {
        JSONObject preferences = user.getUiPreferences();
        preferences.put(propertyName, propertyValue);
        userRepository.setUiPreferences(user, preferences);
        return VisalloResponse.SUCCESS;
    }
}
