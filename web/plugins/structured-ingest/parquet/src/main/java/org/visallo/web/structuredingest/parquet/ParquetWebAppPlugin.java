package org.visallo.web.structuredingest.parquet;

import com.v5analytics.webster.Handler;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;

import javax.servlet.ServletContext;

public class ParquetWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {

        app.registerJavaScript("/org/visallo/web/structuredingest/parquet/js/plugin.js");
        app.registerJavaScript("/org/visallo/web/structuredingest/parquet/js/textSection.js", false);
    }
}
