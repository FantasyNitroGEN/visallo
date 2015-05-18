package org.visallo.web.plugin.requeue;

import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.AuthenticationHandler;
import org.visallo.web.VisalloCsrfHandler;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;
import org.visallo.web.privilegeFilters.EditPrivilegeFilter;
import com.v5analytics.webster.Handler;

import javax.servlet.ServletContext;

@Name("Requeue")
@Description("Allows requeueing an element")
public class RequeueWebAppPlugin implements WebAppPlugin {
    @Override
    @SuppressWarnings("unchecked")
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticator = AuthenticationHandler.class;
        Class<? extends Handler> csrfProtector = VisalloCsrfHandler.class;

        app.registerJavaScript("/org/visallo/web/plugin/requeue/requeue.js");
        app.post("/requeue/vertex", authenticator, csrfProtector, EditPrivilegeFilter.class, RequeueVertex.class);
    }
}
