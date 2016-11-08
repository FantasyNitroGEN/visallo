#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.auth;

import com.google.inject.Inject;
import com.v5analytics.webster.Handler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.AuthenticationHandler;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Example Visallo Authentication Plugin")
@Description("Registers an authentication plugin which demonstrates user/password login.")
public class ExampleAuthenticationPlugin implements WebAppPlugin {
    private final Login login;

    @Inject
    public ExampleAuthenticationPlugin(Login login) {
        this.login = login;
    }

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        app.registerBeforeAuthenticationJavaScript("/${packageInPathFormat}/auth/plugin.js");
        app.registerJavaScript("/${packageInPathFormat}/auth/authentication.js", false);
        app.registerJavaScriptTemplate("/${packageInPathFormat}/auth/login.hbs");
        app.registerCss("/${packageInPathFormat}/auth/login.css");
        app.registerResourceBundle("/${packageInPathFormat}/auth/messages.properties");

        app.post(AuthenticationHandler.LOGIN_PATH, login);
    }
}
