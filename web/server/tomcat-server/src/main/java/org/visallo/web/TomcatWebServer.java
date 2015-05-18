package org.visallo.web;

import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;

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
        httpsConnector.setAttribute("truststoreFile", super.getTrustStorePath());
        httpsConnector.setAttribute("truststorePass", super.getTrustStorePassword());
        httpsConnector.setAttribute("clientAuth", super.getRequireClientCert() ? "true" : "false");
        httpsConnector.setAttribute("sslProtocol", "TLS");
        httpsConnector.setAttribute("SSLEnabled", true);

        tomcat.setPort(super.getHttpPort());
        tomcat.getService().addConnector(httpsConnector);

        Connector defaultConnector = tomcat.getConnector();
        defaultConnector.setRedirectPort(super.getHttpsPort());

        Context context = tomcat.addWebapp(this.getContextPath(), getWebAppDir().getAbsolutePath());
        context.setSessionTimeout(super.getSessionTimeout());
        LOGGER.info("getSessionTimeout() is %d minutes", context.getSessionTimeout());

        System.out.println("configuring app with basedir: " + new File("./" + getWebAppDir()).getAbsolutePath());

        tomcat.start();
        tomcat.getServer().await();

        return 0;
    }

    protected Tomcat getServer() {
        return tomcat;
    }
}
