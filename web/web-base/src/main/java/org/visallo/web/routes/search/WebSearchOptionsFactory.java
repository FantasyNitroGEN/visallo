package org.visallo.web.routes.search;

import org.visallo.core.model.search.SearchOptions;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class WebSearchOptionsFactory {
    public static SearchOptions create(HttpServletRequest request, String workspaceId) {
        Map<String, Object> parameters = new HashMap<>();
        copyRequestAttributesToParameters(request, parameters);
        copyRequestParametersToParameters(request, parameters);
        return new SearchOptions(parameters, workspaceId);
    }

    private static void copyRequestParametersToParameters(HttpServletRequest request, Map<String, Object> parameters) {
        Map<String, String[]> requestParameters = request.getParameterMap();
        for (Map.Entry<String, String[]> requestParameter : requestParameters.entrySet()) {
            String[] requestParameterValues = requestParameter.getValue();
            Object value;
            if (requestParameterValues.length == 0) {
                value = null;
            } else if (requestParameterValues.length == 1) {
                value = requestParameterValues[0];
            } else {
                value = requestParameterValues;
            }
            parameters.put(requestParameter.getKey(), value);
        }
    }

    private static void copyRequestAttributesToParameters(HttpServletRequest request, Map<String, Object> parameters) {
        Enumeration<String> attributeNames = request.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = request.getAttribute(attributeName);
            parameters.put(attributeName, attributeValue);
        }
    }
}
