package org.visallo.web.routes.config;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.json.JSONObject;
import org.visallo.web.VisalloResponse;

import java.util.ResourceBundle;

public class Configuration implements ParameterizedHandler {
    private final org.visallo.core.config.Configuration configuration;

    @Inject
    public Configuration(final org.visallo.core.config.Configuration configuration) {
        this.configuration = configuration;
    }

    @Handle
    public void handle(ResourceBundle resourceBundle, VisalloResponse response) throws Exception {
        JSONObject configuration = this.configuration.toJSON(resourceBundle);
        response.respondWithJson(configuration);
    }
}
