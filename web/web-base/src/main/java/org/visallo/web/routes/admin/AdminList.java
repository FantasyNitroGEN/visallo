package org.visallo.web.routes.admin;

import com.v5analytics.webster.App;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.Route;
import com.v5analytics.webster.annotations.ContentType;
import com.v5analytics.webster.annotations.Handle;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminList implements ParameterizedHandler {

    @Handle
    @ContentType("text/html")
    public String handle(HttpServletRequest request) throws Exception {
        App app = App.getApp(request);
        List<String> paths = getPaths(app);
        return pathsToHtml(paths);
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
