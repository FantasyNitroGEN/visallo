package org.visallo.web.auth.usernameonly;

import com.v5analytics.webster.Handler;
import com.v5analytics.webster.handlers.StaticResourceHandler;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.AuthenticationHandler;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;
import org.visallo.web.auth.usernameonly.routes.Login;

import javax.servlet.ServletContext;

@Name("Username Only Authentication")
@Description("Allows authenticating using just a username")
public class UsernameOnlyWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {

        app.registerBeforeAuthenticationJavaScript("/org/visallo/web/auth/usernameonly/plugin.js");
        app.registerJavaScriptTemplate("/org/visallo/web/auth/usernameonly/templates/login.hbs");
        app.registerJavaScript("/org/visallo/web/auth/usernameonly/authentication.js", false);

        app.registerLess("/org/visallo/web/auth/usernameonly/less/login.less");

        app.post(AuthenticationHandler.LOGIN_PATH, InjectHelper.getInstance(Login.class));
    }
}
