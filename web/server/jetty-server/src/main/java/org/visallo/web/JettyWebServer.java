package org.visallo.web;

import com.beust.jcommander.Parameter;
import com.v5analytics.simpleorm.SimpleOrmJettySessionHandler;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.session.VisalloSimpleOrmJettySessionManager;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;

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
        int httpsPort = super.getHttpsPort();
        int httpPort = super.getHttpPort();

        server = new org.eclipse.jetty.server.Server();

        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(httpsPort);

        ServerConnector httpConnector = new ServerConnector(
                server,
                new HttpConnectionFactory(http_config)
        );
        httpConnector.setPort(httpPort);

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
        httpsConnector.setPort(httpsPort);

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setClassLoader(Thread.currentThread().getContextClassLoader());
        webAppContext.setContextPath(this.getContextPath());
        webAppContext.setWar(getWebAppDir().getAbsolutePath());
        webAppContext.setSessionHandler(new SimpleOrmJettySessionHandler(new VisalloSimpleOrmJettySessionManager()));
        webAppContext.getSessionHandler().getSessionManager().setMaxInactiveInterval(super.getSessionTimeout() * 60);
        LOGGER.info("getMaxInactiveInterval() is %d seconds", webAppContext.getSessionHandler().getSessionManager().getMaxInactiveInterval());

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[]{webAppContext});

        server.setConnectors(new Connector[]{httpConnector, httpsConnector});
        server.setHandler(contexts);

        server.start();

        String message = String.format("Listening on http port %d and https port %d", httpPort, httpsPort);
        LOGGER.info(message);
        System.out.println(message);

        if (!dontJoin) {
            server.join();
        }

        return 0;
    }

    protected org.eclipse.jetty.server.Server getServer() {
        return server;
    }
}
