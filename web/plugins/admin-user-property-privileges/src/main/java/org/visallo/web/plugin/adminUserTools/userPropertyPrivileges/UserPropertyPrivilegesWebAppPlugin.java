package org.visallo.web.plugin.adminUserTools.userPropertyPrivileges;

import com.v5analytics.webster.Handler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.VisalloCsrfHandler;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;
import org.visallo.web.privilegeFilters.AdminPrivilegeFilter;

import javax.servlet.ServletContext;

@Name("Admin User Tools: User Property Privileges")
@Description("Admin tools to manage privileges stored in a property on the user")
public class UserPropertyPrivilegesWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = VisalloCsrfHandler.class;

        app.registerJavaScript("/org/visallo/web/plugin/adminUserTools/userPropertyPrivileges/plugin.js", true);
        app.registerWebWorkerJavaScript(
                "/org/visallo/web/plugin/adminUserTools/userPropertyPrivileges/userAdminPrivilegesService.js"
        );
        app.registerJavaScriptComponent(
                "/org/visallo/web/plugin/adminUserTools/userPropertyPrivileges/UserAdminPrivilegesPlugin.jsx"
        );
        app.registerResourceBundle("/org/visallo/web/plugin/adminUserTools/userPropertyPrivileges/messages.properties");

        app.post(
                "/user/privileges/update",
                authenticationHandlerClass,
                csrfHandlerClass,
                AdminPrivilegeFilter.class,
                UserUpdatePrivileges.class
        );
    }
}
