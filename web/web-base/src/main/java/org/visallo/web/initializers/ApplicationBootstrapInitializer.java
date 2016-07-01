package org.visallo.web.initializers;

import javax.servlet.ServletContext;

public abstract class ApplicationBootstrapInitializer {
    public abstract void initialize(ServletContext context);
}
