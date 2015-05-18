package org.visallo.sql.web;

import com.google.inject.Inject;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import com.v5analytics.webster.Handler;
import org.visallo.sql.model.HibernateSessionManager;
import org.visallo.web.ApplicationBootstrap;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import java.util.EnumSet;

public class SqlModelWebAppPlugin implements WebAppPlugin {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(SqlModelWebAppPlugin.class);
    public static final String FILTER_NAME = "hibernate-session-manager";
    private HibernateSessionManager sessionManager;

    @Inject
    public void configure(HibernateSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        FilterRegistration.Dynamic filter = servletContext.addFilter(FILTER_NAME, new HibernateSessionManagementFilter(sessionManager));
        addMapping(filter, ApplicationBootstrap.VISALLO_SERVLET_NAME);
        addMapping(filter, ApplicationBootstrap.ATMOSPHERE_SERVLET_NAME);
        // TODO: servletContext.getServletRegistrations().keySet() includes atmosphere but not visallo?
        filter.setAsyncSupported(true);
    }

    private void addMapping(FilterRegistration.Dynamic filter, String servletName) {
        LOGGER.info("mapping %s filter for servlet %s", FILTER_NAME, servletName);
        filter.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), false, servletName);
    }
}
