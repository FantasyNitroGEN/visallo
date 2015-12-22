package org.visallo.web.plugin.adminImportRdf;

import com.v5analytics.webster.Handler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.AuthenticationHandler;
import org.visallo.web.VisalloCsrfHandler;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;
import org.visallo.web.privilegeFilters.AdminPrivilegeFilter;

import javax.servlet.ServletContext;

@Name("Admin: RDF Import")
@Description("Import RDF from a file")
public class ImportRdfWebAppPlugin implements WebAppPlugin {
    @Override
    @SuppressWarnings("unchecked")
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticator = AuthenticationHandler.class;
        Class<? extends Handler> csrfProtector = VisalloCsrfHandler.class;

        app.registerJavaScript("/org/visallo/web/plugin/adminImportRdf/plugin.js");
        app.registerJavaScript("/org/visallo/web/plugin/adminImportRdf/admin-import-rdf.js", false);

        app.registerJavaScriptTemplate("/org/visallo/web/plugin/adminImportRdf/templates/admin-import-rdf.hbs");

        app.registerWebWorkerJavaScript("/org/visallo/web/plugin/adminImportRdf/web-worker/adminImportRdf-service.js");

        app.registerResourceBundle("/org/visallo/web/plugin/adminImportRdf/messages.properties");

        app.post("/admin/import-rdf", authenticator, csrfProtector, AdminPrivilegeFilter.class, ImportRdf.class);
    }
}

