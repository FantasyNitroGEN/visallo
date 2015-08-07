package org.visallo.web;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.SessionSupport;
import org.atmosphere.interceptor.HeartbeatInterceptor;
import org.vertexium.Graph;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.bootstrap.VisalloBootstrap;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.externalResource.ExternalResourceRunner;
import org.visallo.core.ingest.graphProperty.GraphPropertyRunner;
import org.visallo.core.ingest.video.VideoFrameInfo;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRunner;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.ServletSecurity;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class ApplicationBootstrap implements ServletContextListener {
    public static final String CONFIG_HTTP_TRANSPORT_GUARANTEE = "http.transportGuarantee";
    private static VisalloLogger LOGGER;
    public static final String APP_CONFIG_LOADER = "application.config.loader";
    public static final String VISALLO_SERVLET_NAME = "visallo";
    public static final String ATMOSPHERE_SERVLET_NAME = "atmosphere";
    public static final String DEBUG_FILTER_NAME = "debug";
    public static final String CACHE_FILTER_NAME = "cache";
    public static final String GZIP_FILTER_NAME = "gzip";
    private UserRepository userRepository;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            final ServletContext context = sce.getServletContext();
            System.out.println("Servlet context initialized...");

            if (context == null) {
                throw new RuntimeException("Failed to initialize context. Visallo is not running.");
            }
            VisalloLoggerFactory.setProcessType("web");

            final Configuration config = ConfigurationLoader.load(context.getInitParameter(APP_CONFIG_LOADER), getInitParametersAsMap(context));
            config.setDefaults(WebConfiguration.DEFAULTS);
            LOGGER = VisalloLoggerFactory.getLogger(ApplicationBootstrap.class);
            LOGGER.info("Running application with configuration:\n%s", config);

            setupInjector(context, config);
            verifyGraphVersion();
            setupGraphAuthorizations();
            setupWebApp(context, config);
            setupLongRunningProcessRunner(config);
            setupGraphPropertyWorkerRunner(config);
            setupExternalResourceWorkers(config);
        } catch (Throwable ex) {
            LOGGER.error("Could not startup context", ex);
            throw new VisalloException("Could not startup context", ex);
        }
    }

    private void verifyGraphVersion() {
        GraphRepository graphRepository = InjectHelper.getInstance(GraphRepository.class);
        graphRepository.verifyVersion();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        safeLogInfo("BEGIN: Servlet context destroyed...");

        safeLogInfo("Shutdown: SimpleOrmSession");
        InjectHelper.getInstance(SimpleOrmSession.class).close();

        safeLogInfo("Shutdown: Graph");
        InjectHelper.getInstance(Graph.class).shutdown();

        safeLogInfo("Shutdown: InjectHelper");
        InjectHelper.shutdown();

        safeLogInfo("Shutdown: VisalloBootstrap");
        VisalloBootstrap.shutdown();

        safeLogInfo("END: Servlet context destroyed...");
    }

    private void safeLogInfo(String message) {
        if (LOGGER != null) {
            LOGGER.info("%s", message);
        } else {
            System.out.println(message);
        }
    }

    @Inject
    public void setUserRepository(UserRepository userProvider) {
        this.userRepository = userProvider;
    }

    private void setupInjector(ServletContext context, Configuration config) {
        LOGGER.debug("setupInjector");
        InjectHelper.inject(this, VisalloBootstrap.bootstrapModuleMaker(config), config);

        // Store the injector in the context for a servlet to access later
        context.setAttribute(Injector.class.getName(), InjectHelper.getInjector());

        InjectHelper.getInstance(OntologyRepository.class); // verify we are up
    }

    private void setupGraphAuthorizations() {
        LOGGER.debug("setupGraphAuthorizations");
        AuthorizationRepository authorizationRepository = InjectHelper.getInstance(AuthorizationRepository.class);
        authorizationRepository.addAuthorizationToGraph(
                VisalloVisibility.SUPER_USER_VISIBILITY_STRING,
                UserRepository.VISIBILITY_STRING,
                TermMentionRepository.VISIBILITY_STRING,
                LongRunningProcessRepository.VISIBILITY_STRING,
                OntologyRepository.VISIBILITY_STRING,
                WorkspaceRepository.VISIBILITY_STRING,
                VideoFrameInfo.VISIBILITY_STRING
        );
    }

    private void setupWebApp(ServletContext context, Configuration config) {
        LOGGER.debug("setupWebApp");
        Router router = new Router(context);
        ServletRegistration.Dynamic servlet = context.addServlet(VISALLO_SERVLET_NAME, router);
        servlet.addMapping("/*");
        servlet.setAsyncSupported(true);
        addSecurityConstraint(servlet, config);
        addAtmosphereServlet(context, config);
        addDebugFilter(context);
        addCacheFilter(context);
        addGzipFilter(context);
        LOGGER.warn("JavaScript / Less modifications will not be reflected on server. Run `grunt` from webapp directory in development");
    }

    private void addAtmosphereServlet(ServletContext context, Configuration config) {
        ServletRegistration.Dynamic servlet = context.addServlet(ATMOSPHERE_SERVLET_NAME, AtmosphereServlet.class);
        context.addListener(SessionSupport.class);
        servlet.addMapping("/messaging/*");
        servlet.setAsyncSupported(true);
        servlet.setLoadOnStartup(0);
        servlet.setInitParameter(AtmosphereHandler.class.getName(), Messaging.class.getName());
        servlet.setInitParameter("org.atmosphere.cpr.sessionSupport", "true");
        servlet.setInitParameter("org.atmosphere.cpr.broadcastFilterClasses", MessagingFilter.class.getName());
        servlet.setInitParameter(AtmosphereInterceptor.class.getName(), HeartbeatInterceptor.class.getName());
        servlet.setInitParameter("org.atmosphere.interceptor.HeartbeatInterceptor.heartbeatFrequencyInSeconds", "30");
        servlet.setInitParameter("org.atmosphere.cpr.CometSupport.maxInactiveActivity", "-1");
        servlet.setInitParameter("org.atmosphere.cpr.broadcasterCacheClass", UUIDBroadcasterCache.class.getName());
        servlet.setInitParameter("org.atmosphere.websocket.maxTextMessageSize", "1048576");
        servlet.setInitParameter("org.atmosphere.websocket.maxBinaryMessageSize", "1048576");
        addSecurityConstraint(servlet, config);
    }

    private void addDebugFilter(ServletContext context) {
        FilterRegistration.Dynamic filter = context.addFilter(DEBUG_FILTER_NAME, RequestDebugFilter.class);
        filter.setAsyncSupported(true);
        filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
    }

    private void addCacheFilter(ServletContext context) {
        FilterRegistration.Dynamic filter = context.addFilter(CACHE_FILTER_NAME, CacheServletFilter.class);
        filter.setAsyncSupported(true);
        String[] mappings = new String[]{"/", "*.html", "*.css", "*.js", "*.ejs", "*.less", "*.hbs", "*.map"};
        for (String mapping : mappings) {
            filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, mapping);
        }
    }

    private void addGzipFilter(ServletContext context) {
        final String GZIP_FILTER_CLASS_NAME = "org.mortbay.servlet.GzipFilter";
        Class<? extends Filter> filterClass;
        try {
            //noinspection unchecked
            filterClass = (Class<? extends Filter>) Class.forName(GZIP_FILTER_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Could not find " + GZIP_FILTER_CLASS_NAME + " not using GZIP compression.", e);
            return;
        }
        FilterRegistration.Dynamic filter = context.addFilter(GZIP_FILTER_NAME, filterClass);
        filter.setInitParameter("mimeTypes", "application/json,text/html,text/plain,text/xml,application/xhtml+xml,text/css,application/javascript,image/svg+xml");
        String[] mappings = new String[]{"/*"};
        for (String mapping : mappings) {
            filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, mapping);
        }
    }

    private void addSecurityConstraint(ServletRegistration.Dynamic servletRegistration, Configuration config) {
        ServletSecurity.TransportGuarantee transportGuarantee = ServletSecurity.TransportGuarantee.CONFIDENTIAL;
        String constraintType = config.get(CONFIG_HTTP_TRANSPORT_GUARANTEE, null);
        if (constraintType != null) {
            transportGuarantee = ServletSecurity.TransportGuarantee.valueOf(constraintType);
        }

        HttpConstraintElement httpConstraintElement = new HttpConstraintElement(transportGuarantee);
        ServletSecurityElement securityElement = new ServletSecurityElement(httpConstraintElement);
        servletRegistration.setServletSecurity(securityElement);
    }

    private Map<String, String> getInitParametersAsMap(ServletContext context) {
        Map<String, String> initParameters = new HashMap<>();
        Enumeration<String> e = context.getInitParameterNames();
        while (e.hasMoreElements()) {
            String initParameterName = e.nextElement();
            initParameters.put(initParameterName, context.getInitParameter(initParameterName));
        }
        return initParameters;
    }

    private void setupLongRunningProcessRunner(final Configuration config) {
        LOGGER.debug("setupLongRunningProcessRunner");

        boolean enabled = Boolean.parseBoolean(config.get(Configuration.WEB_APP_EMBEDDED_LONG_RUNNING_PROCESS_RUNNER_ENABLED, Boolean.toString(Configuration.WEB_APP_EMBEDDED_LONG_RUNNING_PROCESS_RUNNER_ENABLED_DEFAULT)));
        if (!enabled) {
            LOGGER.debug("skipping embedded long running process runners");
            return;
        }

        int threadCount = Integer.parseInt(config.get(Configuration.WEB_APP_EMBEDDED_LONG_RUNNING_PROCESS_RUNNER_THREAD_COUNT, Integer.toString(Configuration.WEB_APP_EMBEDDED_LONG_RUNNING_PROCESS_RUNNER_THREAD_COUNT_DEFAULT)));

        LOGGER.debug("long running process runners: %d", threadCount);
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    delayStart();
                    final LongRunningProcessRunner longRunningProcessRunner = InjectHelper.getInstance(LongRunningProcessRunner.class);
                    longRunningProcessRunner.prepare(config.toMap());
                    try {
                        longRunningProcessRunner.run();
                    } catch (Exception ex) {
                        LOGGER.error("Failed running long running process runner", ex);
                    }
                }
            });
            t.setName("long-running-process-runner-" + t.getId());
            t.setDaemon(true);
            LOGGER.debug("starting long running process runner thread: %s", t.getName());
            t.start();
        }
    }

    private void setupGraphPropertyWorkerRunner(Configuration config) {
        LOGGER.debug("setupGraphPropertyWorkerRunner");

        boolean enabled = Boolean.parseBoolean(config.get(Configuration.WEB_APP_EMBEDDED_GRAPH_PROPERTY_WORKER_RUNNER_ENABLED, Boolean.toString(Configuration.WEB_APP_EMBEDDED_GRAPH_PROPERTY_WORKER_RUNNER_ENABLED_DEFAULT)));
        if (!enabled) {
            LOGGER.debug("skipping embedded graph property worker");
            return;
        }

        int threadCount = Integer.parseInt(config.get(Configuration.WEB_APP_EMBEDDED_GRAPH_PROPERTY_WORKER_RUNNER_THREAD_COUNT, Integer.toString(Configuration.WEB_APP_EMBEDDED_GRAPH_PROPERTY_WORKER_RUNNER_THREAD_COUNT_DEFAULT)));
        final User user = userRepository.getSystemUser();

        LOGGER.debug("starting graph property worker runners: %d", threadCount);
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    delayStart();
                    GraphPropertyRunner graphPropertyRunner = InjectHelper.getInstance(GraphPropertyRunner.class);
                    graphPropertyRunner.prepare(user);
                    try {
                        graphPropertyRunner.run();
                    } catch (Exception ex) {
                        LOGGER.error("Failed running graph property runner", ex);
                    }
                }
            });
            t.setName("graph-property-worker-runner-" + t.getId());
            t.setDaemon(true);
            LOGGER.debug("starting graph property worker runner thread: %s", t.getName());
            t.start();
        }
    }

    private void setupExternalResourceWorkers(Configuration config) {
        LOGGER.debug("setupExternalResourceWorkers");

        boolean enabled = Boolean.parseBoolean(config.get(Configuration.WEB_APP_EMBEDDED_EXTERNAL_RESOURCE_WORKERS_ENABLED, Boolean.toString(Configuration.WEB_APP_EMBEDDED_EXTERNAL_RESOURCE_WORKERS_ENABLED_DEFAULT)));
        if (!enabled) {
            LOGGER.debug("skipping external resource worker");
            return;
        }

        final User user = userRepository.getSystemUser();
        new ExternalResourceRunner(config, user).startAll();
    }

    /**
     * Delay the start of GPW and long running processes so the web app comes up faster
     */
    private void delayStart() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            LOGGER.error("Could not sleep", e);
        }
    }
}
