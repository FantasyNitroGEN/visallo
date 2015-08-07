package org.visallo.yarn;

import com.beust.jcommander.JCommander;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.bootstrap.VisalloBootstrap;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.hadoop.yarn.api.records.ContainerExitStatus;

public abstract class TaskBase {
    private Configuration configuration;
    private InjectHelper.ModuleMaker moduleMaker;
    private UserRepository userRepository;
    private User user;

    public final void run(String[] args) {
        VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(TaskBase.class);
        new JCommander(this, args);

        ClientBase.printEnv();
        ClientBase.printSystemProperties();

        try {
            LOGGER.info("BEGIN Run");
            this.configuration = ConfigurationLoader.load();
            this.moduleMaker = VisalloBootstrap.bootstrapModuleMaker(configuration);
            this.userRepository = InjectHelper.getInstance(UserRepository.class, getModuleMaker(), getConfiguration());
            this.user = getUserRepository().getSystemUser();

            run();
            LOGGER.info("END Run");
            System.exit(ContainerExitStatus.SUCCESS);
        } catch (Throwable ex) {
            LOGGER.error("FAILED Run", ex);
            System.exit(1);
        }
    }

    protected abstract void run() throws Exception;

    public Configuration getConfiguration() {
        return configuration;
    }

    public InjectHelper.ModuleMaker getModuleMaker() {
        return moduleMaker;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public User getUser() {
        return user;
    }
}
