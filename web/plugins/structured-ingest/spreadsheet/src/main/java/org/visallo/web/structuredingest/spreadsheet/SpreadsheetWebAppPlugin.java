package org.visallo.web.structuredingest.spreadsheet;

import com.v5analytics.webster.Handler;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;

import javax.servlet.ServletContext;

public class SpreadsheetWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {

        app.registerJavaScript("/org/visallo/web/structuredingest/spreadsheet/plugin.js");

    }
}
