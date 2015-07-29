package org.visallo.opennlpDictionary.web;

import com.v5analytics.webster.Handler;
import com.v5analytics.webster.handlers.StaticResourceHandler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.VisalloCsrfHandler;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;
import org.visallo.web.privilegeFilters.AdminPrivilegeFilter;

import javax.servlet.ServletContext;

@Name("Dictionary Editor")
@Description("Allows editing a dictionary of terms")
public class AdminDictionaryWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = VisalloCsrfHandler.class;

        app.registerJavaScriptTemplate("/org/visallo/opennlpDictionary/web/templates/add.hbs");

        app.registerJavaScript("/org/visallo/opennlpDictionary/web/plugin.js");
        app.registerJavaScript("/org/visallo/opennlpDictionary/web/list-plugin.js", false);
        app.registerJavaScript("/org/visallo/opennlpDictionary/web/add-plugin.js", false);

        app.get("/admin/dictionary", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, AdminDictionary.class);
        app.get("/admin/dictionary/concept", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, AdminDictionaryByConcept.class);
        app.post("/admin/dictionary", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, AdminDictionaryEntryAdd.class);
        app.post("/admin/dictionary/delete", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, AdminDictionaryEntryDelete.class);
    }
}
