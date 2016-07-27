package org.visallo.web.plugin.adminUserTools;

import com.v5analytics.webster.Handler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.VisalloCsrfHandler;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;
import org.visallo.web.privilegeFilters.AdminPrivilegeFilter;

import javax.servlet.ServletContext;

@Name("Admin User Tools")
@Description("Admin tools to add/update/delete users")
public class AdminUserToolsWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = VisalloCsrfHandler.class;

        app.registerJavaScript("/org/visallo/web/adminUserTools/plugin.js");

        app.registerJavaScriptComponent("/org/visallo/web/adminUserTools/UserAdminPlugin.jsx");
        app.registerJavaScriptComponent("/org/visallo/web/adminUserTools/WorkspaceList.jsx");
        app.registerJavaScriptComponent("/org/visallo/web/adminUserTools/LoadUser.jsx");
        app.registerJavaScriptComponent("/org/visallo/web/adminUserTools/UserTypeaheadInput.jsx");
        app.registerCss("/org/visallo/web/adminUserTools/userAdmin.css");
        app.registerCss("/org/visallo/web/adminUserTools/workspaceList.css");

        app.registerResourceBundle("/org/visallo/web/adminUserTools/messages.properties");

        app.post("/user/delete", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, UserDelete.class);
        app.post("/workspace/shareWithMe", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, WorkspaceShareWithMe.class);
    }
}
