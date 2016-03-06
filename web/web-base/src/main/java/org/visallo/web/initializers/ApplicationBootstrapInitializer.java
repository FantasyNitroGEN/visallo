package org.visallo.web.initializers;

import javax.servlet.ServletContext;
import java.io.Closeable;

public abstract class ApplicationBootstrapInitializer implements Closeable {
    public abstract void initialize(ServletContext context);
}
