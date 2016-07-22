package org.visallo.web.plugin.adminUserTools.userPropertyAuth;

import com.v5analytics.webster.Handler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.VisalloCsrfHandler;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;
import org.visallo.web.privilegeFilters.AdminPrivilegeFilter;

import javax.servlet.ServletContext;

@Name("Admin User Tools: User Property Authorization")
@Description("Admin tools to manage authorizations stored in a property on the user")
public class UserPropertyAuthorizationWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = VisalloCsrfHandler.class;

        app.registerJavaScript("/org/visallo/web/plugin/adminUserTools/userPropertyAuth/plugin.js", true);
        app.registerJavaScriptComponent(
                "/org/visallo/web/plugin/adminUserTools/userPropertyAuth/UserAdminAuthorizationPlugin.jsx"
        );
        app.registerWebWorkerJavaScript("/org/visallo/web/plugin/adminUserTools/userPropertyAuth/userAdminAuthorizationService.js");
        app.registerResourceBundle("/org/visallo/web/plugin/adminUserTools/userPropertyAuth/messages.properties");

        app.post(
                "/user/auth/add",
                authenticationHandlerClass,
                csrfHandlerClass,
                AdminPrivilegeFilter.class,
                UserAddAuthorization.class
        );
        app.post(
                "/user/auth/remove",
                authenticationHandlerClass,
                csrfHandlerClass,
                AdminPrivilegeFilter.class,
                UserRemoveAuthorization.class
        );
    }
}
