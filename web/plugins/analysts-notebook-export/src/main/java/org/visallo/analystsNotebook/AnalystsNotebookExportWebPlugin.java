package org.visallo.analystsNotebook;

import org.visallo.analystsNotebook.routes.AnalystsNotebookExport;
import com.v5analytics.webster.Handler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.VisalloCsrfHandler;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;
import org.visallo.web.privilegeFilters.ReadPrivilegeFilter;

import javax.servlet.ServletContext;

@Name("Analyst's Notebook")
@Description("Adds exporting to Analyst's Notebook")
public class AnalystsNotebookExportWebPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = VisalloCsrfHandler.class;

        app.registerJavaScript("/org/visallo/analystsNotebook/analystsNotebook.js");
        app.registerResourceBundle("/org/visallo/analystsNotebook/messages.properties");

        app.get("/analysts-notebook/export", authenticationHandlerClass, csrfHandlerClass, ReadPrivilegeFilter.class, AnalystsNotebookExport.class);
    }
}
