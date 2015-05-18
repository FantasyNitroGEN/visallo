package org.visallo.web.auth.usernamepassword;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import com.v5analytics.webster.Handler;
import com.v5analytics.webster.handlers.StaticResourceHandler;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;
import org.visallo.web.WebConfiguration;
import org.visallo.web.auth.usernamepassword.routes.Login;
import org.visallo.web.auth.usernamepassword.routes.ChangePassword;
import org.visallo.web.auth.usernamepassword.routes.LookupToken;
import org.visallo.web.auth.usernamepassword.routes.RequestToken;
import org.visallo.web.AuthenticationHandler;

import javax.servlet.ServletContext;

@Name("Username/Password Authentication")
@Description("Allows authenticating using a username and password")
public class UsernamePasswordWebAppPlugin implements WebAppPlugin {
    public static final String LOOKUP_TOKEN_ROUTE = "/forgotPassword";
    public static final String CHANGE_PASSWORD_ROUTE = "/forgotPassword/changePassword";
    private Configuration configuration;

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        StaticResourceHandler jsHandler = new StaticResourceHandler(this.getClass(), "/username-password/authentication.js", "application/javascript");
        StaticResourceHandler loginTemplateHandler = new StaticResourceHandler(this.getClass(), "/username-password/templates/login.hbs", "text/plain");
        StaticResourceHandler lessHandler = new StaticResourceHandler(this.getClass(), "/username-password/less/login.less", "text/plain");

        app.get("/jsc/configuration/plugins/authentication/authentication.js", jsHandler);
        app.get("/jsc/configuration/plugins/authentication/templates/login.hbs", loginTemplateHandler);
        app.get("/jsc/configuration/plugins/authentication/less/login.less", lessHandler);

        app.post(AuthenticationHandler.LOGIN_PATH, InjectHelper.getInstance(Login.class));

        ForgotPasswordConfiguration forgotPasswordConfiguration = new ForgotPasswordConfiguration();
        configuration.setConfigurables(forgotPasswordConfiguration, ForgotPasswordConfiguration.CONFIGURATION_PREFIX);
        configuration.set(WebConfiguration.PREFIX + ForgotPasswordConfiguration.CONFIGURATION_PREFIX + ".enabled", forgotPasswordConfiguration.isEnabled());
        if (forgotPasswordConfiguration.isEnabled()) {
            app.post("/forgotPassword/requestToken", RequestToken.class);
            app.get(LOOKUP_TOKEN_ROUTE, LookupToken.class);
            app.post(CHANGE_PASSWORD_ROUTE, ChangePassword.class);
        }
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
