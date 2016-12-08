package org.visallo.web.table;

import com.v5analytics.webster.Handler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Table")
@Description("Provides a dashboard card for tabular saved search results")
public class TableWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {

        app.registerJavaScript("/org/visallo/web/table/js/plugin.js", true);
        app.registerCompiledJavaScript("/org/visallo/web/table/dist/card.js");
        app.registerJavaScriptComponent("/org/visallo/web/table/js/card/Config.jsx");
        app.registerJavaScriptTemplate("/org/visallo/web/table/hbs/columnConfigPopover.hbs");

        app.registerCss("/org/visallo/web/table/node_modules/react-virtualized/styles.css");
        app.registerCss("/org/visallo/web/table/node_modules/react-resizable/css/styles.css");
        app.registerLess("/org/visallo/web/table/less/table.less");

        app.registerResourceBundle("/org/visallo/web/table/messages.properties");

        app.registerFile("/org/visallo/web/table/img/empty-table.png", "image/png");
    }
}
