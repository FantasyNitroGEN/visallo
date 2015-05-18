package org.visallo.web.auth;

import com.v5analytics.webster.Handler;
import com.v5analytics.webster.handlers.StaticResourceHandler;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.AuthenticationHandler;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("X.509+LDAP Authentication")
@Description("Allows authenticating using an X.509 Certificate and LDAP server")
public class LdapX509WebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        StaticResourceHandler jsHandler = new StaticResourceHandler(this.getClass(), "/ldap-x509/authentication.js", "application/javascript");
        StaticResourceHandler loginTemplateHandler = new StaticResourceHandler(this.getClass(), "/ldap-x509/templates/login.hbs", "text/plain");
        StaticResourceHandler lessHandler = new StaticResourceHandler(this.getClass(), "/ldap-x509/less/login.less", "text/plain");

        app.registerJavaScript("/ldap-x509/logout.js");

        app.get("/logout.html", new StaticResourceHandler(this.getClass(), "/ldap-x509/logout.html", "text/html"));
        app.get("/jsc/configuration/plugins/authentication/css/logout.css", new StaticResourceHandler(this.getClass(), "/ldap-x509/css/logout.css", "text/css"));
        app.get("/jsc/configuration/plugins/authentication/authentication.js", jsHandler);
        app.get("/jsc/configuration/plugins/authentication/templates/login.hbs", loginTemplateHandler);
        app.get("/jsc/configuration/plugins/authentication/less/login.less", lessHandler);

        app.post(AuthenticationHandler.LOGIN_PATH, InjectHelper.getInstance(LdapX509AuthenticationHandler.class));
    }
}
