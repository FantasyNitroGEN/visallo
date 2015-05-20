package org.visallo.web;

import com.google.inject.Binder;
import org.visallo.core.bootstrap.BootstrapBindingProvider;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserSessionCounterRepository;
import org.visallo.web.session.WebCuratorUserSessionCounterRepository;

/**
 * Configure dependency injection only for the web application.
 */
public class WebAppBootstrapBindingProvider implements BootstrapBindingProvider {
    @Override
    public void addBindings(Binder binder, Configuration configuration) {
        // Override the default.
        binder.bind(UserSessionCounterRepository.class)
                .to(WebCuratorUserSessionCounterRepository.class);
    }
}
