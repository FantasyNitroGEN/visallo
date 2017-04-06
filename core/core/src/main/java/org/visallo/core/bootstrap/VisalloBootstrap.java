package org.visallo.core.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.vertexium.Graph;
import org.visallo.core.config.Configuration;
import org.visallo.core.email.EmailRepository;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.geocoding.GeocoderRepository;
import org.visallo.core.http.HttpRepository;
import org.visallo.core.model.directory.DirectoryRepository;
import org.visallo.core.model.file.FileSystemRepository;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.model.user.*;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.ACLProvider;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.status.JmxMetricsManager;
import org.visallo.core.status.MetricsManager;
import org.visallo.core.status.StatusRepository;
import org.visallo.core.time.TimeRepository;
import org.visallo.core.trace.TraceRepository;
import org.visallo.core.trace.Traced;
import org.visallo.core.trace.TracedMethodInterceptor;
import org.visallo.core.user.User;
import org.visallo.core.util.ServiceLoaderUtil;
import org.visallo.core.util.ShutdownService;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The VisalloBootstrap is a Guice Module that configures itself by
 * discovering all available implementations of BootstrapBindingProvider
 * and invoking the addBindings() method.  If any discovered provider
 * cannot be instantiated, configuration of the Bootstrap Module will
 * fail and halt application initialization by throwing a BootstrapException.
 */
public class VisalloBootstrap extends AbstractModule {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VisalloBootstrap.class);
    private static final String GRAPH_METADATA_VISALLO_GRAPH_VERSION_KEY = "visallo.graph.version";
    private static final Integer GRAPH_METADATA_VISALLO_GRAPH_VERSION = 3;

    private static VisalloBootstrap visalloBootstrap;

    public synchronized static VisalloBootstrap bootstrap(final Configuration configuration) {
        if (visalloBootstrap == null) {
            LOGGER.debug("Initializing VisalloBootstrap with Configuration:\n%s", configuration);
            visalloBootstrap = new VisalloBootstrap(configuration);
        }
        return visalloBootstrap;
    }

    /**
     * Get a ModuleMaker that will return the VisalloBootstrap, initializing it with
     * the provided Configuration if it has not already been created.
     *
     * @param configuration the Visallo configuration
     * @return a ModuleMaker for use with the InjectHelper
     */
    public static InjectHelper.ModuleMaker bootstrapModuleMaker(final Configuration configuration) {
        return new InjectHelper.ModuleMaker() {
            @Override
            public Module createModule() {
                return VisalloBootstrap.bootstrap(configuration);
            }

            @Override
            public Configuration getConfiguration() {
                return configuration;
            }
        };
    }

    /**
     * The Visallo Configuration.
     */
    private final Configuration configuration;

    /**
     * Create a VisalloBootstrap with the provided Configuration.
     *
     * @param config the configuration for this bootstrap
     */
    private VisalloBootstrap(final Configuration config) {
        this.configuration = config;
    }

    @Override
    protected void configure() {
        LOGGER.info("Configuring VisalloBootstrap.");

        checkNotNull(configuration, "configuration cannot be null");
        bind(Configuration.class).toInstance(configuration);

        LOGGER.debug("binding %s", JmxMetricsManager.class.getName());
        MetricsManager metricsManager = new JmxMetricsManager();
        bind(MetricsManager.class).toInstance(metricsManager);

        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Traced.class), new TracedMethodInterceptor());

        bind(TraceRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.TRACE_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(Graph.class)
                .toProvider(getGraphProvider(configuration, Configuration.GRAPH_PROVIDER))
                .in(Scopes.SINGLETON);
        bind(LockRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.LOCK_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(WorkQueueRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.WORK_QUEUE_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(LongRunningProcessRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.LONG_RUNNING_PROCESS_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(DirectoryRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.DIRECTORY_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(VisibilityTranslator.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.VISIBILITY_TRANSLATOR))
                .in(Scopes.SINGLETON);
        bind(UserRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.USER_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(UserSessionCounterRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.USER_SESSION_COUNTER_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(SearchRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.SEARCH_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(WorkspaceRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.WORKSPACE_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(GraphAuthorizationRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.GRAPH_AUTHORIZATION_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(OntologyRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.ONTOLOGY_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(SimpleOrmSession.class)
                .toProvider(getSimpleOrmSessionProvider(configuration, Configuration.SIMPLE_ORM_SESSION))
                .in(Scopes.SINGLETON);
        bind(HttpRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.HTTP_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(GeocoderRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.GEOCODER_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(EmailRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.EMAIL_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(StatusRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.STATUS_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(ACLProvider.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.ACL_PROVIDER_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(FileSystemRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.FILE_SYSTEM_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(AuthorizationRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.AUTHORIZATION_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(PrivilegeRepository.class)
                .toProvider(VisalloBootstrap.getConfigurableProvider(configuration, Configuration.PRIVILEGE_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(TimeRepository.class)
                .toInstance(new TimeRepository());
        injectProviders();
    }

    private Provider<? extends SimpleOrmSession> getSimpleOrmSessionProvider(
            Configuration configuration,
            String simpleOrmSessionConfigurationName
    ) {
        return (Provider<SimpleOrmSession>) () -> {
            Provider<? extends SimpleOrmSession> provider = VisalloBootstrap.getConfigurableProvider(
                    configuration,
                    simpleOrmSessionConfigurationName
            );
            SimpleOrmSession simpleOrmSession = provider.get();
            getShutdownService().register(new SimpleOrmSessionShutdownListener(simpleOrmSession));
            return simpleOrmSession;
        };
    }

    private Provider<? extends Graph> getGraphProvider(Configuration configuration, String configurationPrefix) {
        // TODO change to use org.vertexium.GraphFactory
        String graphClassName = configuration.get(configurationPrefix, null);
        if (graphClassName == null) {
            throw new VisalloException("Could not find graph configuration: " + configurationPrefix);
        }
        final Map<String, String> configurationSubset = configuration.getSubset(configurationPrefix);

        final Class<?> graphClass;
        try {
            LOGGER.debug("Loading graph class \"%s\"", graphClassName);
            graphClass = Class.forName(graphClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find graph class with name: " + graphClassName, e);
        }

        final Method createMethod;
        try {
            createMethod = graphClass.getDeclaredMethod("create", Map.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find create(Map) method on class: " + graphClass.getName(), e);
        }

        return (Provider<Graph>) () -> {
            Graph g;
            try {
                LOGGER.debug("creating graph");
                g = (Graph) createMethod.invoke(null, configurationSubset);
            } catch (Exception e) {
                LOGGER.error("Could not create graph %s", graphClass.getName(), e);
                throw new VisalloException("Could not create graph " + graphClass.getName(), e);
            }

            checkVisalloGraphVersion(g);

            getShutdownService().register(new GraphShutdownListener(g));
            return g;
        };
    }

    private ShutdownService getShutdownService() {
        return InjectHelper.getInstance(ShutdownService.class);
    }

    public void checkVisalloGraphVersion(Graph g) {
        Object visalloGraphVersionObj = g.getMetadata(GRAPH_METADATA_VISALLO_GRAPH_VERSION_KEY);
        if (visalloGraphVersionObj == null) {
            g.setMetadata(GRAPH_METADATA_VISALLO_GRAPH_VERSION_KEY, GRAPH_METADATA_VISALLO_GRAPH_VERSION);
        } else if (visalloGraphVersionObj instanceof Integer) {
            Integer visalloGraphVersion = (Integer) visalloGraphVersionObj;
            if (!GRAPH_METADATA_VISALLO_GRAPH_VERSION.equals(visalloGraphVersion)) {
                throw new VisalloException("Invalid " + GRAPH_METADATA_VISALLO_GRAPH_VERSION_KEY + " expected " + GRAPH_METADATA_VISALLO_GRAPH_VERSION + " found " + visalloGraphVersion);
            }
        } else {
            throw new VisalloException("Invalid " + GRAPH_METADATA_VISALLO_GRAPH_VERSION_KEY + " expected Integer found " + visalloGraphVersionObj.getClass().getName());
        }
    }

    private void injectProviders() {
        LOGGER.info("Running %s", BootstrapBindingProvider.class.getName());
        Iterable<BootstrapBindingProvider> bindingProviders = ServiceLoaderUtil.loadWithoutInjecting(BootstrapBindingProvider.class, configuration);
        for (BootstrapBindingProvider provider : bindingProviders) {
            LOGGER.debug("Configuring bindings from BootstrapBindingProvider: %s", provider.getClass().getName());
            provider.addBindings(this.binder(), configuration);
        }
    }

    public static void shutdown() {
        visalloBootstrap = null;
    }

    public static <T> Provider<? extends T> getConfigurableProvider(final Configuration config, final String key) {
        Class<? extends T> configuredClass = config.getClass(key);
        return configuredClass != null ? new ConfigurableProvider<>(configuredClass, config, key, null) : new NullProvider<>();
    }

    private static class NullProvider<T> implements Provider<T> {
        @Override
        public T get() {
            return null;
        }
    }

    private static class ConfigurableProvider<T> implements Provider<T> {
        private final Class<? extends T> clazz;
        private final Method initMethod;
        private final Object[] initMethodArgs;
        private final Configuration config;
        private final String keyPrefix;

        public ConfigurableProvider(final Class<? extends T> clazz, final Configuration config, String keyPrefix, final User user) {
            this.config = config;
            this.keyPrefix = keyPrefix;
            Method init;
            Object[] initArgs = null;
            init = findInit(clazz, Configuration.class, User.class);
            if (init != null) {
                initArgs = new Object[]{config, user};
            } else {
                init = findInit(clazz, Map.class, User.class);
                if (init != null) {
                    initArgs = new Object[]{config.toMap(), user};
                } else {
                    init = findInit(clazz, Configuration.class);
                    if (init != null) {
                        initArgs = new Object[]{config};
                    } else {
                        init = findInit(clazz, Map.class);
                        if (init != null) {
                            initArgs = new Object[]{config.toMap()};
                        }
                    }
                }
            }
            this.clazz = clazz;
            this.initMethod = init;
            this.initMethodArgs = initArgs;
        }

        private Method findInit(Class<? extends T> target, Class<?>... paramTypes) {
            try {
                return target.getMethod("init", paramTypes);
            } catch (NoSuchMethodException ex) {
                return null;
            } catch (SecurityException ex) {
                List<String> paramNames = new ArrayList<>();
                for (Class<?> pc : paramTypes) {
                    paramNames.add(pc.getSimpleName());
                }
                throw new VisalloException(String.format("Error accessing init(%s) method in %s.", paramNames, clazz.getName()), ex);
            }
        }

        @Override
        public T get() {
            Throwable error;
            try {
                LOGGER.debug("creating %s", this.clazz.getName());
                T impl;
                if (InjectHelper.getInjector() != null) {
                    impl = InjectHelper.getInstance(this.clazz);
                } else {
                    Constructor<? extends T> constructor = this.clazz.getConstructor();
                    impl = constructor.newInstance();
                }
                if (initMethod != null) {
                    initMethod.invoke(impl, initMethodArgs);
                }
                config.setConfigurables(impl, this.keyPrefix);
                return impl;
            } catch (IllegalAccessException iae) {
                LOGGER.error("Unable to access default constructor for %s", clazz.getName(), iae);
                error = iae;
            } catch (IllegalArgumentException iae) {
                LOGGER.error("Unable to initialize instance of %s.", clazz.getName(), iae);
                error = iae;
            } catch (InvocationTargetException ite) {
                LOGGER.error("Error initializing instance of %s.", clazz.getName(), ite);
                error = ite;
            } catch (NoSuchMethodException e) {
                LOGGER.error("Could not find constructor for %s.", clazz.getName(), e);
                error = e;
            } catch (InstantiationException e) {
                LOGGER.error("Could not create %s.", clazz.getName(), e);
                error = e;
            }
            throw new VisalloException(String.format("Unable to initialize instance of %s", clazz.getName()), error);
        }
    }
}
