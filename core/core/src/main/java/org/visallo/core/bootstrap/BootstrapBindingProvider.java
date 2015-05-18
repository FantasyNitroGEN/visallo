package org.visallo.core.bootstrap;

import org.visallo.core.config.Configuration;
import com.google.inject.Binder;

/**
 * A BootstrapBindingProvider can add Guice bindings to the Visallo Bootstrap Module.
 * Implementations are automatically discovered by the Visallo Bootstrapper and will be
 * instantiated using an empty constructor.
 */
public interface BootstrapBindingProvider {
    /**
     * Add the bindings defined by this BootstrapBindingProvider to
     * the Visallo Bootstrap module.
     * @param binder the Binder that configures the Bootstrapper
     * @param configuration the Visallo Configuration
     */
    void addBindings(final Binder binder, final Configuration configuration);
}
