package org.visallo.web;

import org.apache.catalina.Context;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.File;

public class TomcatWebServer extends WebServer {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(TomcatWebServer.class);
    private Tomcat tomcat;

    public static void main(String[] args) throws Exception {
        main(new TomcatWebServer(), args, false);
    }

    @Override
    protected int run() throws Exception {
        tomcat = new Tomcat();

        Connector httpsConnector = new Connector(Http11NioProtocol.class.getName());
        httpsConnector.setPort(super.getHttpsPort());
        httpsConnector.setSecure(true);
        httpsConnector.setScheme("https");
        httpsConnector.setAttribute("keystoreFile", super.getKeyStorePath());
        httpsConnector.setAttribute("keystorePass", super.getKeyStorePassword());
        httpsConnector.setAttribute("keystoreType", super.getKeyStoreType());
        httpsConnector.setAttribute("truststoreFile", super.getTrustStorePath());
        httpsConnector.setAttribute("truststorePass", super.getTrustStorePassword());
        httpsConnector.setAttribute("truststoreType", super.getTrustStoreType());
        httpsConnector.setAttribute("sslProtocol", "TLS");
        httpsConnector.setAttribute("SSLEnabled", true);
        setClientCertHandling(httpsConnector);

        tomcat.setPort(super.getHttpPort());
        tomcat.getService().addConnector(httpsConnector);

        Connector defaultConnector = tomcat.getConnector();
        defaultConnector.setRedirectPort(super.getHttpsPort());

        Context context = tomcat.addWebapp(this.getContextPath(), getWebAppDir().getAbsolutePath());
        context.setSessionTimeout(super.getSessionTimeout());

        StandardJarScanner jarScanner = new StandardJarScanner();
        jarScanner.setScanClassPath(false);
        context.setJarScanner(jarScanner);

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

    public void setClientCertHandling(Connector httpsConnector) {
        if (getRequireClientCert() && getWantClientCert()) {
            throw new IllegalArgumentException("Choose only one of --requireClientCert and --wantClientCert");
        }
        if (getRequireClientCert()) {
            httpsConnector.setAttribute("clientAuth", "true");
        } else if (getWantClientCert()) {
            httpsConnector.setAttribute("clientAuth", "want");
        } else {
            httpsConnector.setAttribute("clientAuth", "false");
        }
    }
}
