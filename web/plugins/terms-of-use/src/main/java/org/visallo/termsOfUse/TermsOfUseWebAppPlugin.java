package org.visallo.termsOfUse;

import com.v5analytics.webster.Handler;
import com.v5analytics.webster.handlers.StaticResourceHandler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Terms of Use")
@Description("Shows a user term of use")
public class TermsOfUseWebAppPlugin implements WebAppPlugin {
    public static final String TERMS_OF_USE_PATH = "/terms";

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {

        app.get("/jsc/org/visallo/termsOfUse/terms-of-use.hbs",
                new StaticResourceHandler(getClass(), "/org/visallo/termsOfUse/terms-of-use.hbs", "text/html"));

        app.registerJavaScript("/org/visallo/termsOfUse/terms-of-use-plugin.js");
        app.registerResourceBundle("/org/visallo/termsOfUse/messages.properties");
        app.get(TERMS_OF_USE_PATH, TermsOfUse.class);
        app.post(TERMS_OF_USE_PATH, TermsOfUse.class);
    }
}
