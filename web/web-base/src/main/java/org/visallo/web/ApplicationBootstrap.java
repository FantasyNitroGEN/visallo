package org.visallo.web;

import com.google.inject.Injector;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.cpr.*;
import org.atmosphere.interceptor.HeartbeatInterceptor;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.bootstrap.VisalloBootstrap;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.video.VideoFrameInfo;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.GraphAuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.util.ServiceLoaderUtil;
import org.visallo.core.util.ShutdownService;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.initializers.ApplicationBootstrapInitializer;

import javax.servlet.*;
import javax.servlet.annotation.ServletSecurity;
import java.util.*;

public class ApplicationBootstrap implements ServletContextListener {
    private static VisalloLogger LOGGER;

    public static final String CONFIG_HTTP_TRANSPORT_GUARANTEE = "http.transportGuarantee";
    public static final String APP_CONFIG_LOADER = "application.config.loader";
    public static final String VISALLO_SERVLET_NAME = "visallo";
    public static final String ATMOSPHERE_SERVLET_NAME = "atmosphere";
    public static final String DEBUG_FILTER_NAME = "debug";
    public static final String CACHE_FILTER_NAME = "cache";
    private volatile boolean isStopped = false;
    private Configuration config;
    private List<ApplicationBootstrapInitializer> applicationBootstrapInitializers = new ArrayList<>();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            final ServletContext context = sce.getServletContext();

            if (context == null) {
                throw new RuntimeException("Failed to initialize context. Visallo is not running.");
            }
            VisalloLoggerFactory.setProcessType("web");

            Map<String, String> initParameters = new HashMap<>(getInitParametersAsMap(context));
            initParameters.putAll(WebConfiguration.DEFAULTS);
            config = ConfigurationLoader.load(context.getInitParameter(APP_CONFIG_LOADER), initParameters);
            LOGGER = VisalloLoggerFactory.getLogger(ApplicationBootstrap.class);
            LOGGER.info("Running application with configuration:\n%s", config);

            setupInjector(context, config);
            verifyGraphVersion();
            setupGraphAuthorizations();

            Iterable<ApplicationBootstrapInitializer> initializers =
                    ServiceLoaderUtil.load(ApplicationBootstrapInitializer.class, config);
            for (ApplicationBootstrapInitializer initializer : initializers) {
                initializer.initialize(context);
                applicationBootstrapInitializers.add(initializer);
            }

            setupWebApp(context, config);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    contextDestroyed(null);
                }
            });
        } catch (Throwable ex) {
            if (LOGGER != null) {
                LOGGER.error("Could not startup context", ex);
            }
            throw new VisalloException("Could not startup context", ex);
        }
    }

    private void verifyGraphVersion() {
        GraphRepository graphRepository = InjectHelper.getInstance(GraphRepository.class);
        graphRepository.verifyVersion();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (isStopped) {
            return;
        }
        isStopped = true;

        InjectHelper.getInstance(ShutdownService.class).shutdown();
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
        GraphAuthorizationRepository graphAuthorizationRepository = InjectHelper.getInstance(GraphAuthorizationRepository.class);
        graphAuthorizationRepository.addAuthorizationToGraph(
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
        addMultiPartConfig(config, servlet);
        addSecurityConstraint(servlet, config);
        addAtmosphereServlet(context, config);
        addDebugFilter(context);
        addCacheFilter(context);

        LOGGER.info(
                "JavaScript / Less modifications will not be reflected on server. Run `grunt` from webapp directory in development");
    }

    private void addMultiPartConfig(Configuration config, ServletRegistration.Dynamic servlet) {
        String location = config.get(Configuration.MULTIPART_LOCATION, Configuration.DEFAULT_MULTIPART_LOCATION);
        long maxFileSize = config.getLong(Configuration.MULTIPART_MAX_FILE_SIZE, Configuration.DEFAULT_MULTIPART_MAX_FILE_SIZE);
        long maxRequestSize = config.getLong(Configuration.MULTIPART_MAX_REQUEST_SIZE, Configuration.DEFAULT_MULTIPART_MAX_REQUEST_SIZE);
        int fileSizeThreshold = config.getInt(Configuration.MULTIPART_FILE_SIZE_THRESHOLD, Configuration.DEFAULT_MULTIPART_FILE_SIZE_THRESHOLD);

        servlet.setMultipartConfig(
                new MultipartConfigElement(location, maxFileSize, maxRequestSize, fileSizeThreshold)
        );
    }

    private void addAtmosphereServlet(ServletContext context, Configuration config) {
        ServletRegistration.Dynamic servlet = context.addServlet(ATMOSPHERE_SERVLET_NAME, AtmosphereServlet.class);
        context.addListener(SessionSupport.class);
        servlet.addMapping(Messaging.PATH + "/*");
        servlet.setAsyncSupported(true);
        servlet.setLoadOnStartup(0);
        servlet.setInitParameter(AtmosphereHandler.class.getName(), Messaging.class.getName());
        servlet.setInitParameter(ApplicationConfig.PROPERTY_SESSION_CREATE, "false");
        servlet.setInitParameter(ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true");
        servlet.setInitParameter(ApplicationConfig.BROADCAST_FILTER_CLASSES, MessagingFilter.class.getName() + "," +
                MessagingThrottleFilter.class.getName());
        servlet.setInitParameter(AtmosphereInterceptor.class.getName(), HeartbeatInterceptor.class.getName());
        servlet.setInitParameter(ApplicationConfig.HEARTBEAT_INTERVAL_IN_SECONDS, "30");
        servlet.setInitParameter(ApplicationConfig.MAX_INACTIVE, "-1");
        servlet.setInitParameter(ApplicationConfig.BROADCASTER_CACHE, UUIDBroadcasterCache.class.getName());
        servlet.setInitParameter(ApplicationConfig.DROP_ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "true");
        servlet.setInitParameter(ApplicationConfig.WEBSOCKET_MAXTEXTSIZE, "1048576");
        servlet.setInitParameter(ApplicationConfig.WEBSOCKET_MAXBINARYSIZE, "1048576");

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
        String[] mappings = new String[]{"/", "*.html", "*.css", "*.js", "*.ejs", "*.less", "*.hbs"};
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
}
