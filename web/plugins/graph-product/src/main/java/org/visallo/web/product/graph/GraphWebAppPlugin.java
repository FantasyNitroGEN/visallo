package org.visallo.web.product.graph;

import com.v5analytics.webster.Handler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Product: Graph")
@Description("Graph visualization")
public class GraphWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {

        app.registerJavaScript("/org/visallo/web/product/graph/plugin.js");

        app.registerCompiledJavaScript("/org/visallo/web/product/graph/dist/Graph.js");
        app.registerCompiledJavaScript("/org/visallo/web/product/graph/dist/EdgeLabel.js");
        app.registerCompiledJavaScript("/org/visallo/web/product/graph/dist/SnapToGrid.js");
        app.registerCompiledJavaScript("/org/visallo/web/product/graph/dist/FindPathPopoverContainer.js");
        app.registerCompiledJavaScript("/org/visallo/web/product/graph/dist/actions-impl.js");

        app.registerCompiledWebWorkerJavaScript("/org/visallo/web/product/graph/dist/plugin-worker.js");
        app.registerCompiledWebWorkerJavaScript("/org/visallo/web/product/graph/dist/store-changes.js");

        app.registerLess("/org/visallo/web/product/graph/css.less");
        app.registerResourceBundle("/org/visallo/web/product/graph/messages.properties");
        app.registerFile("/org/visallo/web/product/graph/select-arrow.png", "image/png");
    }
}
