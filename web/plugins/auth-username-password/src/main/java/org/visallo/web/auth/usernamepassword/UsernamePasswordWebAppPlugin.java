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
        app.registerBeforeAuthenticationJavaScript("/org/visallo/web/auth/usernamepassword/plugin.js");
        app.registerJavaScriptTemplate("/org/visallo/web/auth/usernamepassword/templates/login.hbs");
        app.registerJavaScript("/org/visallo/web/auth/usernamepassword/authentication.js", false);

        app.registerLess("/org/visallo/web/auth/usernamepassword/less/login.less");

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
