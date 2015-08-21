package org.visallo.web.devTools.routes;

import com.v5analytics.webster.handlers.RouteRunner;
import org.apache.commons.io.IOUtils;
import org.visallo.core.exception.VisalloException;

public class VisalloRouteRunner extends RouteRunner {
    private String additionalJavascript;

    @Override
    protected String getPageTitle() {
        return "Visallo";
    }

    protected String getAdditionalJavascript() {
        if (additionalJavascript == null) {
            try {
                additionalJavascript = IOUtils.toString(VisalloRouteRunner.class.getResourceAsStream("visalloRouteRunner.js"));
            } catch (Exception e) {
                throw new VisalloException("Could not load visalloRouteRunner.js", e);
            }
        }
        return additionalJavascript;
    }
}
