package org.visallo.web.devTools;

import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.web.devTools.ontology.SaveOntologyProperty;
import com.v5analytics.webster.Handler;
import org.visallo.web.VisalloCsrfHandler;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;
import org.visallo.web.devTools.ontology.SaveOntologyConcept;
import org.visallo.web.devTools.ontology.SaveOntologyRelationship;
import org.visallo.web.devTools.routes.*;
import org.visallo.web.devTools.user.*;
import org.visallo.web.privilegeFilters.AdminPrivilegeFilter;

import javax.servlet.ServletContext;

@Name("Development Tools")
@Description("A collection of tools for developers")
public class DevToolsWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = VisalloCsrfHandler.class;

        app.registerJavaScript("/org/visallo/web/devTools/plugin.js");

        app.registerJavaScript("/org/visallo/web/devTools/route-runner-plugin.js", false);
        app.registerJavaScript("/org/visallo/web/devTools/element-editor-plugin.js", false);
        app.registerJavaScript("/org/visallo/web/devTools/requeue-plugin.js", false);
        app.registerJavaScript("/org/visallo/web/devTools/ontology-upload-plugin.js", false);
        app.registerJavaScript("/org/visallo/web/devTools/ontology-edit-concept-plugin.js", false);
        app.registerJavaScript("/org/visallo/web/devTools/ontology-edit-property-plugin.js", false);
        app.registerJavaScript("/org/visallo/web/devTools/ontology-edit-relationship-plugin.js", false);
        app.registerJavaScript("/org/visallo/web/devTools/user-plugin.js", false);

        app.registerWebWorkerJavaScript("/org/visallo/web/devTools/web-worker/devTools-service.js");

        app.registerJavaScriptTemplate("/org/visallo/web/devTools/templates/element-editor.hbs");
        app.registerJavaScriptTemplate("/org/visallo/web/devTools/templates/requeue.hbs");
        app.registerJavaScriptTemplate("/org/visallo/web/devTools/templates/ontology-upload.hbs");
        app.registerJavaScriptTemplate("/org/visallo/web/devTools/templates/ontology-edit-concept.hbs");
        app.registerJavaScriptTemplate("/org/visallo/web/devTools/templates/ontology-edit-property.hbs");
        app.registerJavaScriptTemplate("/org/visallo/web/devTools/templates/ontology-edit-relationship.hbs");
        app.registerJavaScriptTemplate("/org/visallo/web/devTools/templates/user.hbs");
        app.registerJavaScriptTemplate("/org/visallo/web/devTools/templates/user-details.hbs");

        app.registerLess("/org/visallo/web/devTools/less/element-editor.less");

        app.registerResourceBundle("/org/visallo/web/devTools/messages.properties");

        app.post("/user/auth/add", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, UserAddAuthorization.class);
        app.post("/user/auth/remove", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, UserRemoveAuthorization.class);
        app.post("/user/delete", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, UserDelete.class);
        app.post("/user/privileges/update", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, UserUpdatePrivileges.class);

        app.post("/workspace/shareWithMe", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, WorkspaceShareWithMe.class);

        app.get("/admin/routeRunner", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, VisalloRouteRunner.class);
        app.post("/admin/queueVertices", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, QueueVertices.class);
        app.post("/admin/queueEdges", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, QueueEdges.class);
        app.post("/admin/deleteVertex", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, DeleteVertex.class);
        app.post("/admin/deleteEdge", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, DeleteEdge.class);

        app.post("/org/visallo/web/devTools/saveOntologyConcept", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, SaveOntologyConcept.class);
        app.post("/org/visallo/web/devTools/saveOntologyProperty", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, SaveOntologyProperty.class);
        app.post("/org/visallo/web/devTools/saveOntologyRelationship", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, SaveOntologyRelationship.class);
    }
}
