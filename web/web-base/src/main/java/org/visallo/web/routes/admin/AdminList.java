package org.visallo.web.routes.admin;

import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.web.BaseRequestHandler;
import com.v5analytics.webster.App;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.Route;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminList extends BaseRequestHandler {
    @Inject
    public AdminList(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        App app = App.getApp(request);
        List<String> paths = getPaths(app);
        String out = pathsToHtml(paths);
        respondWithHtml(response, out);
    }

    private String pathsToHtml(List<String> paths) {
        StringBuilder out = new StringBuilder();
        out.append("<html>");
        out.append("<head>");
        out.append("  <title>Visallo: Admin Index</title>");
        out.append("</head>");
        out.append("<body>");
        out.append("  <ul>");
        for (String path : paths) {
            out.append("    <li><a href='" + path + "'>" + path + "</a></li>");
        }
        out.append("  </ul>");
        out.append("</body>");
        out.append("</html>");
        return out.toString();
    }

    private List<String> getPaths(App app) {
        Map<Route.Method, List<Route>> routes = app.getRouter().getRoutes();
        List<String> paths = new ArrayList<String>();
        for (Map.Entry<Route.Method, List<Route>> routeByMethod : routes.entrySet()) {
            if (routeByMethod.getKey() != Route.Method.GET) {
                continue;
            }
            for (Route route : routeByMethod.getValue()) {
                if (!route.getPath().startsWith("/admin/")) {
                    continue;
                }
                paths.add(route.getPath());
            }
        }
        return paths;
    }
}
