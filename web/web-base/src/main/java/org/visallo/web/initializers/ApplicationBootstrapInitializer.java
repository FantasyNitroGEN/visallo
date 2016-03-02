package org.visallo.web.initializers;

import java.io.Closeable;

public abstract class ApplicationBootstrapInitializer implements Closeable {
    public abstract void initialize();
}
