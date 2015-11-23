package org.visallo.core.cmdline;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.inject.Inject;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.bootstrap.VisalloBootstrap;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VersionUtil;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public abstract class CommandLineTool {
    protected VisalloLogger LOGGER;
    public static final boolean DEFAULT_INIT_FRAMEWORK = true;
    private Configuration configuration;
    private boolean willExit = false;
    private UserRepository userRepository;
    private Authorizations authorizations;
    private LockRepository lockRepository;
    private User user;
    private Graph graph;
    private WorkQueueRepository workQueueRepository;
    private OntologyRepository ontologyRepository;
    private VisibilityTranslator visibilityTranslator;
    private SimpleOrmSession simpleOrmSession;

    @Parameter(names = {"--help", "-h"}, description = "Print help", help = true)
    private boolean help;

    @Parameter(names = {"--version"}, description = "Print version")
    private boolean version;

    public int run(String[] args) throws Exception {
        return run(args, DEFAULT_INIT_FRAMEWORK);
    }

    public int run(String[] args, boolean initFramework) throws Exception {
        LOGGER = VisalloLoggerFactory.getLogger(CommandLineTool.class, "cli");
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                willExit = true;
                try {
                    mainThread.join(1000);
                } catch (InterruptedException e) {
                    // nothing useful to do here
                }
            }
        });

        try {
            JCommander j = parseArguments(args);
            if (j == null) {
                return -1;
            }
            if (help) {
                printHelp(j);
                return -1;
            }
            if (version) {
                VersionUtil.printVersion();
                return -1;
            }
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            return -1;
        }

        if (initFramework) {
            InjectHelper.inject(this, VisalloBootstrap.bootstrapModuleMaker(getConfiguration()), getConfiguration());
        }

        int result = -1;
        try {
            result = run();
            LOGGER.debug("command result: %d", result);
        } finally {
            if (initFramework) {
                shutdown();
            }
        }
        return result;
    }

    protected void printHelp(JCommander j) {
        j.usage();
    }

    protected JCommander parseArguments(String[] args) {
        return new JCommander(this, args);
    }

    protected void shutdown() {
        if (this.lockRepository != null) {
            LOGGER.debug("shutting down %s", this.lockRepository.getClass().getName());
            this.lockRepository.shutdown();
        }
        if (graph != null) {
            LOGGER.debug("shutting down %s", this.graph.getClass().getName());
            this.graph.shutdown();
        }
        if (this.workQueueRepository != null) {
            LOGGER.debug("shutting down %s", this.workQueueRepository.getClass().getName());
            this.workQueueRepository.shutdown();
        }
        if (this.simpleOrmSession != null) {
            LOGGER.debug("shutting down %s", this.simpleOrmSession.getClass().getName());
            this.simpleOrmSession.close();
        }
    }

    protected abstract int run() throws Exception;

    protected Configuration getConfiguration() {
        if (configuration == null) {
            configuration = ConfigurationLoader.load();
        }
        return configuration;
    }

    protected User getUser() {
        if (this.user == null) {
            this.user = userRepository.getSystemUser();
        }
        return this.user;
    }

    protected Authorizations getAuthorizations() {
        if (this.authorizations == null) {
            this.authorizations = this.userRepository.getAuthorizations(getUser());
        }
        return this.authorizations;
    }

    protected boolean willExit() {
        return willExit;
    }

    @Inject
    public void setLockRepository(LockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }

    @Inject
    public final void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public final void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Inject
    public final void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    @Inject
    public final void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Inject
    public final void setSimpleOrmSession(SimpleOrmSession simpleOrmSession) {
        this.simpleOrmSession = simpleOrmSession;
    }

    public SimpleOrmSession getSimpleOrmSession() {
        return simpleOrmSession;
    }

    public Graph getGraph() {
        return graph;
    }

    public WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public OntologyRepository getOntologyRepository() {
        return ontologyRepository;
    }

    public VisibilityTranslator getVisibilityTranslator() {
        return visibilityTranslator;
    }

    @Inject
    public void setVisibilityTranslator(VisibilityTranslator visibilityTranslator) {
        this.visibilityTranslator = visibilityTranslator;
    }

    public static void main(CommandLineTool commandLineTool, String[] args, boolean initFramework) throws Exception {
        int res = commandLineTool.run(args, initFramework);
        if (res != 0) {
            System.exit(res);
        }
    }

    public static void main(CommandLineTool commandLineTool, String[] args) throws Exception {
        main(commandLineTool, args, DEFAULT_INIT_FRAMEWORK);
    }
}
