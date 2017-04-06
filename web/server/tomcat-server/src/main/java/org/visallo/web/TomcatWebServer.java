package org.visallo.web;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.File;

public class TomcatWebServer extends WebServer {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(TomcatWebServer.class);
    private static final String COMPRESSABLE_MIME_TYPES = String.join(",",
            "application/json",
            "text/html",
            "text/plain",
            "text/xml",
            "application/xhtml+xml",
            "text/css",
            "application/javascript",
            "image/svg+xml",
            "text/javascript"
    );

    private Tomcat tomcat;

    public static void main(String[] args) throws Exception {
        main(new TomcatWebServer(), args, false);
    }

    @Override
    protected int run() throws Exception {
        Connector httpsConnector = new Connector(Http11NioProtocol.class.getName());
        setupSslHandling(httpsConnector);
        setupClientCertHandling(httpsConnector);
        setupCompression(httpsConnector);

        tomcat = new Tomcat();
        tomcat.setPort(super.getHttpPort());
        tomcat.getService().addConnector(httpsConnector);

        Connector defaultConnector = tomcat.getConnector();
        defaultConnector.setRedirectPort(super.getHttpsPort());

        Context context = tomcat.addWebapp(this.getContextPath(), getWebAppDir().getAbsolutePath());

        // don't scan classpath for web components to avoid benign log warnings
        StandardJarScanner jarScanner = new StandardJarScanner();
        jarScanner.setScanClassPath(false);
        context.setJarScanner(jarScanner);

        // establish default caching settings to avoid benign log warnings
        StandardRoot webRoot = new StandardRoot(context);
        webRoot.setCacheMaxSize(100000);
        webRoot.setCachingAllowed(true);
        context.setResources(webRoot);

        LOGGER.info("getSessionTimeout() is %d minutes", context.getSessionTimeout());

        System.out.println("configuring app with basedir: " + new File("./" + getWebAppDir()).getAbsolutePath());

        tomcat.start();
        tomcat.getServer().await();

        return 0;
    }

    protected Tomcat getServer() {
        return tomcat;
    }

    private void setupSslHandling(Connector connector) {
        connector.setPort(super.getHttpsPort());
        connector.setSecure(true);
        connector.setScheme("https");
        connector.setAttribute("keystoreFile", super.getKeyStorePath());
        connector.setAttribute("keystorePass", super.getKeyStorePassword());
        connector.setAttribute("keystoreType", super.getKeyStoreType());
        connector.setAttribute("truststoreFile", super.getTrustStorePath());
        connector.setAttribute("truststorePass", super.getTrustStorePassword());
        connector.setAttribute("truststoreType", super.getTrustStoreType());
        connector.setAttribute("sslProtocol", "TLS");
        connector.setAttribute("SSLEnabled", true);
    }

    public void setupClientCertHandling(Connector httpsConnector) {
        if (getRequireClientCert() && getWantClientCert()) {
            throw new IllegalArgumentException("Choose only one of --requireClientCert and --wantClientCert");
        }

        String clientAuthSetting = "false";
        if (getRequireClientCert()) {
            clientAuthSetting = "true";
        } else if (getWantClientCert()) {
            clientAuthSetting = "want";
        }

        httpsConnector.setAttribute("clientAuth", clientAuthSetting);
        LOGGER.info("clientAuth certificate handling set to %s", clientAuthSetting);
    }

    public void setupCompression(Connector connector) {
        ProtocolHandler handler = connector.getProtocolHandler();
        if (handler instanceof AbstractHttp11Protocol) {
            AbstractHttp11Protocol<?> protocol = (AbstractHttp11Protocol<?>) handler;
            protocol.setCompression("on");
            protocol.setCompressableMimeType(COMPRESSABLE_MIME_TYPES);
            LOGGER.info("compression set for mime types: %s", COMPRESSABLE_MIME_TYPES);
        }
    }
}
