package org.visallo.web.importExportWorkspaces;

import com.v5analytics.webster.Handler;
import com.v5analytics.webster.handlers.StaticResourceHandler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.VisalloCsrfHandler;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;
import org.visallo.web.privilegeFilters.AdminPrivilegeFilter;

import javax.servlet.ServletContext;

@Name("Workspace Import/Export")
@Description("Allows a user to import or export a workspace")
public class ImportExportWorkspaceWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = VisalloCsrfHandler.class;

        app.get("/jsc/org/visallo/web/importExportWorkspaces/import.hbs",
                new StaticResourceHandler(getClass(), "/org/visallo/web/importExportWorkspaces/import.hbs", "text/html"));
        app.registerJavaScript("/org/visallo/web/importExportWorkspaces/import-plugin.js");
        app.registerResourceBundle("/org/visallo/web/importExportWorkspaces/messages.properties");

        app.get("/jsc/org/visallo/web/importExportWorkspaces/export.hbs",
                new StaticResourceHandler(getClass(), "/org/visallo/web/importExportWorkspaces/export.hbs", "text/html"));
        app.registerJavaScript("/org/visallo/web/importExportWorkspaces/export-plugin.js");

        app.get("/admin/workspace/export", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, Export.class);
        app.post("/admin/workspace/import", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, Import.class);
    }
}
