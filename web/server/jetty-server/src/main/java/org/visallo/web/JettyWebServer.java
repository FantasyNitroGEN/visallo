package org.visallo.web;

import com.beust.jcommander.Parameter;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.lang.management.ManagementFactory;

public class JettyWebServer extends WebServer {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(JettyWebServer.class, "web");
    private Server server;

    @Parameter(names = {"--dontjoin"}, description = "Don't join the server thread and continue with exit")
    private boolean dontJoin = false;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new JettyWebServer(), args, false);
    }

    @Override
    protected int run() throws Exception {
        server = new org.eclipse.jetty.server.Server();

        // Setup JMX
        MBeanContainer mbContainer=new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addEventListener(mbContainer);
        server.addBean(mbContainer);
        server.addBean(Log.getLog());

        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(getHttpsPort());

        ServerConnector httpConnector = new ServerConnector(
                server,
                new HttpConnectionFactory(http_config)
        );
        httpConnector.setPort(getHttpPort());

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(getKeyStorePath().getAbsolutePath());
        sslContextFactory.setKeyStorePassword(super.getKeyStorePassword());
        sslContextFactory.setTrustStorePath(getTrustStorePath().getAbsolutePath());
        sslContextFactory.setTrustStorePassword(super.getTrustStorePassword());
        sslContextFactory.setNeedClientAuth(super.getRequireClientCert());

        HttpConfiguration https_config = new HttpConfiguration(http_config);
        https_config.addCustomizer(new SecureRequestCustomizer());

        ServerConnector httpsConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(https_config));
        httpsConnector.setPort(getHttpsPort());

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setClassLoader(Thread.currentThread().getContextClassLoader());
        webAppContext.setContextPath(this.getContextPath());
        webAppContext.setWar(getWebAppDir().getAbsolutePath());
        webAppContext.setSessionHandler(new HashSessionManager().getSessionHandler());
        LOGGER.info("getMaxInactiveInterval() is %d seconds", webAppContext.getSessionHandler().getSessionManager().getMaxInactiveInterval());

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[]{webAppContext});

        server.setConnectors(new Connector[]{httpConnector, httpsConnector});
        server.setHandler(contexts);

        server.start();
        afterServerStart();

        if (!dontJoin) {
            server.join();
        }

        return 0;
    }

    protected org.eclipse.jetty.server.Server getServer() {
        return server;
    }

    protected void afterServerStart() {
        String message = String.format("Listening on http port %d and https port %d", getHttpPort(), getHttpsPort());
        LOGGER.info(message);
        System.out.println(message);
    }
}
