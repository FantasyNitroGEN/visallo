package org.visallo.web.initializers;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.externalResource.ExternalResourceRunner;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.status.StatusRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.Collection;

public class ExternalResourceWorkersInitializer extends ApplicationBootstrapInitializer {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ExternalResourceWorkersInitializer.class);
    private final Configuration config;
    private final UserRepository userRepository;
    private final StatusRepository statusRepository;
    private ExternalResourceRunner resourceRunner;

    @Inject
    public ExternalResourceWorkersInitializer(
            Configuration config,
            UserRepository userRepository,
            StatusRepository statusRepository
    ) {
        this.config = config;
        this.userRepository = userRepository;
        this.statusRepository = statusRepository;
    }

    @Override
    public void initialize(ServletContext context) {
        LOGGER.debug("setupExternalResourceWorkers");

        final User user = userRepository.getSystemUser();
        resourceRunner = new ExternalResourceRunner(config, statusRepository, user);
        resourceRunner.startAll();
    }

    @Override
    public void close() throws IOException {
        if (resourceRunner != null) {
            resourceRunner.shutdown();
        }
    }
}
