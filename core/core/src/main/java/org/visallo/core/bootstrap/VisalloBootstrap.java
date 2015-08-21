package org.visallo.core.bootstrap;

import com.google.inject.*;
import com.google.inject.matcher.Matchers;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.vertexium.Graph;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.geocoding.DefaultGeocoderRepository;
import org.visallo.core.geocoding.GeocoderRepository;
import org.visallo.core.http.DefaultHttpRepository;
import org.visallo.core.http.HttpRepository;
import org.visallo.core.model.lock.CuratorLockRepository;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.status.JmxMetricsManager;
import org.visallo.core.status.MetricsManager;
import org.visallo.core.trace.DefaultTraceRepository;
import org.visallo.core.trace.TraceRepository;
import org.visallo.core.trace.Traced;
import org.visallo.core.trace.TracedMethodInterceptor;
import org.visallo.core.user.User;
import org.visallo.core.util.ClassUtil;
import org.visallo.core.util.ServiceLoaderUtil;
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
    private static final Integer GRAPH_METADATA_VISALLO_GRAPH_VERSION = 1;

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

        LOGGER.debug("binding %s", CuratorFrameworkProvider.class.getName());
        bind(CuratorFramework.class)
                .toProvider(new CuratorFrameworkProvider(configuration))
                .in(Scopes.SINGLETON);

        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Traced.class), new TracedMethodInterceptor());

        bind(TraceRepository.class)
                .toProvider(VisalloBootstrap.<TraceRepository>getConfigurableProvider(configuration, Configuration.TRACE_REPOSITORY, DefaultTraceRepository.class))
                .in(Scopes.SINGLETON);
        bind(Graph.class)
                .toProvider(getGraphProvider(configuration, Configuration.GRAPH_PROVIDER))
                .in(Scopes.SINGLETON);
        bind(LockRepository.class)
                .toProvider(VisalloBootstrap.<LockRepository>getConfigurableProvider(configuration, Configuration.LOCK_REPOSITORY, CuratorLockRepository.class))
                .in(Scopes.SINGLETON);
        bind(WorkQueueRepository.class)
                .toProvider(VisalloBootstrap.<WorkQueueRepository>getConfigurableProvider(configuration, Configuration.WORK_QUEUE_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(LongRunningProcessRepository.class)
                .toProvider(VisalloBootstrap.<LongRunningProcessRepository>getConfigurableProvider(configuration, Configuration.LONG_RUNNING_PROCESS_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(VisibilityTranslator.class)
                .toProvider(VisalloBootstrap.<VisibilityTranslator>getConfigurableProvider(configuration, Configuration.VISIBILITY_TRANSLATOR))
                .in(Scopes.SINGLETON);
        bind(UserRepository.class)
                .toProvider(VisalloBootstrap.<UserRepository>getConfigurableProvider(configuration, Configuration.USER_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(WorkspaceRepository.class)
                .toProvider(VisalloBootstrap.<WorkspaceRepository>getConfigurableProvider(configuration, Configuration.WORKSPACE_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(AuthorizationRepository.class)
                .toProvider(VisalloBootstrap.<AuthorizationRepository>getConfigurableProvider(configuration, Configuration.AUTHORIZATION_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(OntologyRepository.class)
                .toProvider(VisalloBootstrap.<OntologyRepository>getConfigurableProvider(configuration, Configuration.ONTOLOGY_REPOSITORY))
                .in(Scopes.SINGLETON);
        bind(SimpleOrmSession.class)
                .toProvider(VisalloBootstrap.<SimpleOrmSession>getConfigurableProvider(configuration, Configuration.SIMPLE_ORM_SESSION))
                .in(Scopes.SINGLETON);
        bind(HttpRepository.class)
                .toProvider(VisalloBootstrap.<HttpRepository>getConfigurableProvider(configuration, Configuration.HTTP_REPOSITORY, DefaultHttpRepository.class))
                .in(Scopes.SINGLETON);
        bind(GeocoderRepository.class)
                .toProvider(VisalloBootstrap.<GeocoderRepository>getConfigurableProvider(configuration, Configuration.GEOCODER_REPOSITORY, DefaultGeocoderRepository.class))
                .in(Scopes.SINGLETON);

        injectProviders();
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

        return new Provider<Graph>() {
            @Override
            public Graph get() {
                Graph g;
                try {
                    LOGGER.debug("creating graph");
                    g = (Graph) createMethod.invoke(null, configurationSubset);
                } catch (Exception e) {
                    throw new RuntimeException("Could not create graph " + graphClass.getName(), e);
                }

                checkVisalloGraphVersion(g);

                return g;
            }
        };
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
        Iterable<BootstrapBindingProvider> bindingProviders = ServiceLoaderUtil.load(BootstrapBindingProvider.class, configuration);
        for (BootstrapBindingProvider provider : bindingProviders) {
            LOGGER.debug("Configuring bindings from BootstrapBindingProvider: %s", provider.getClass().getName());
            provider.addBindings(this.binder(), configuration);
        }
    }

    public static void shutdown() {
        visalloBootstrap = null;
    }

    private static class CuratorFrameworkProvider implements Provider<CuratorFramework> {
        private String zookeeperConnectionString;
        private RetryPolicy retryPolicy;

        public CuratorFrameworkProvider(Configuration configuration) {
            zookeeperConnectionString = configuration.get(Configuration.ZK_SERVERS, null);
            if (zookeeperConnectionString == null) {
                throw new VisalloException("Could not find configuration item: " + Configuration.ZK_SERVERS);
            }
            retryPolicy = new ExponentialBackoffRetry(1000, 6);
        }

        @Override
        public CuratorFramework get() {
            CuratorFramework client = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
            client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
                @Override
                public void stateChanged(CuratorFramework client, ConnectionState newState) {
                    LOGGER.debug("curator connection state changed to " + newState.name());
                }
            });
            client.start();
            return client;
        }
    }

    public static <T> void bind(Binder binder, Configuration configuration, String propertyKey, Class<T> type, Class<? extends T> defaultClass) {
        String className = configuration.get(propertyKey, defaultClass.getName());
        try {
            Class<? extends T> klass = ClassUtil.forName(className);
            binder.bind(type).to(klass).in(Scopes.SINGLETON);
        } catch (Exception ex) {
            throw new VisalloException("Failed to bind " + className + " as singleton instance of " + type.getName() + "(configure with " + propertyKey + ")", ex);
        }
    }

    public static <T> Provider<? extends T> getConfigurableProvider(final Configuration config, final String key) {
        return getConfigurableProvider(config, key, null);
    }

    public static <T> Provider<? extends T> getConfigurableProvider(final Configuration config, final String key, Class<? extends T> defaultClass) {
        Class<? extends T> configuredClass = config.getClass(key, defaultClass);
        return configuredClass != null ? new ConfigurableProvider<>(configuredClass, config, key, null) : new NullProvider<T>();
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
