package org.visallo.web;

import com.v5analytics.webster.handlers.CSRFHandler;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class VisalloCsrfHandler extends CSRFHandler {
    public static final String PARAMETER_NAME = "csrfToken";
    public static final String HEADER_NAME = "Visallo-CSRF-Token";

    public VisalloCsrfHandler() {
        super(PARAMETER_NAME, HEADER_NAME);
    }
}
