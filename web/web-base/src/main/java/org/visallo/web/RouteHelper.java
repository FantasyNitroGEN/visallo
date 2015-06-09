package org.visallo.web;

import org.visallo.core.config.Configuration;

import javax.servlet.http.HttpServletRequest;

/**
 * This contains various methods used by routes to process parameters, etc.
 */
public class RouteHelper {
    private static final String JUSTIFICATION_TEXT = "justificationText";

    private final Configuration configuration;
    private final MinimalRequestHandler handler;

    public RouteHelper(Configuration configuration, MinimalRequestHandler handler) {
        this.configuration = configuration;
        this.handler = handler;
    }

    public String getJustificationText(HttpServletRequest request) {
        return justificationParameter(isJustificationRequired(), request);
    }

    public String getJustificationText(boolean isComment, String sourceInfo, HttpServletRequest request) {
        return justificationParameter(isJustificationRequired(isComment, sourceInfo), request);
    }

    public boolean isJustificationRequired() {
        return WebConfiguration.justificationRequired(configuration);
    }

    public boolean isJustificationRequired(boolean isComment, String sourceInfo) {
        return !isComment && sourceInfo == null &&  isJustificationRequired();
    }

    private String justificationParameter(boolean required, HttpServletRequest request) {
        return required ?
                handler.getRequiredParameter(request, JUSTIFICATION_TEXT) :
                handler.getOptionalParameter(request, JUSTIFICATION_TEXT);
    }
}
