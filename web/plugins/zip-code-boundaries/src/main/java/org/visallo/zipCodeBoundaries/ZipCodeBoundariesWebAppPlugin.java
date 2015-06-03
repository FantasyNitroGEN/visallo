package org.visallo.zipCodeBoundaries;

import com.v5analytics.webster.Handler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Zip Code Boundaries")
@Description("Get Zip Code Boundaries")
public class ZipCodeBoundariesWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        app.get("/zip-code-boundary", GetZipCodeBoundaries.class);
    }
}
