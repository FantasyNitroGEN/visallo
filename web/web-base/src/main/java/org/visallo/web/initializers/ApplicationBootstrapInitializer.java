package org.visallo.web.initializers;

import javax.servlet.ServletContext;
import java.io.Closeable;
import java.io.IOException;

public abstract class ApplicationBootstrapInitializer implements Closeable {
    public abstract void initialize(ServletContext context);

    @Override
    public void close() throws IOException {
    }
}
