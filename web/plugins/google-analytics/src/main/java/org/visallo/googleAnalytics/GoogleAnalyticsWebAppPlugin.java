package org.visallo.googleAnalytics;

import com.v5analytics.webster.Handler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Google Analytics")
@Description("Adds support for tracking users using Google Analytics")
public class GoogleAnalyticsWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        app.registerJavaScript("/org/visallo/googleAnalytics/google-analytics-plugin.js");
    }
}
