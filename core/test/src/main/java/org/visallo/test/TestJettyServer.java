package org.visallo.test;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.JettyWebServer;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

public class TestJettyServer extends JettyWebServer {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(TestJettyServer.class);
    private final File webAppDir;
    private final int httpPort;
    private final int httpsPort;
    private final String keyStorePath;
    private final String keyStorePassword;

    public TestJettyServer(File webAppDir, int httpPort, int httpsPort, String keyStorePath, String keyStorePassword) {
        this.webAppDir = webAppDir;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        });
    }

    public void startup() {
        checkNotNull(webAppDir, "webAppDir cannot be null");
        checkNotNull(keyStorePath, "keyStorePath cannot be null");
        checkNotNull(keyStorePassword, "keyStorePassword cannot be null");

        String[] args = new String[]{
                "--port",
                Integer.toString(httpPort),
                "--httpsPort",
                Integer.toString(httpsPort),
                "--keyStorePath",
                keyStorePath,
                "--keyStorePassword",
                keyStorePassword,
                "--webAppDir",
                webAppDir.getAbsolutePath(),
                "--dontjoin"
        };

        try {
            LOGGER.info("Running Jetty on http port " + httpPort + ", https port " + httpsPort);
            LOGGER.info("   args: %s", Joiner.on(' ').skipNulls().join(args));
            int code = this.run(args, false);
            if (code != 0) {
                throw new RuntimeException("Jetty failed to startup");
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    public void shutdown() {
        LOGGER.info("shutdown");
        try {
            if (this.getServer() != null) {
                this.getServer().stop();
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }
}
