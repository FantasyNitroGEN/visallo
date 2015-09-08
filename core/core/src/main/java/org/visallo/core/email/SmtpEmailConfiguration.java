package org.visallo.core.email;

import org.visallo.core.config.Configurable;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.PostConfigurationValidator;

public class SmtpEmailConfiguration {
    public static final String CONFIGURATION_PREFIX = Configuration.EMAIL_REPOSITORY + ".smtp";

    private String serverHostname;
    private int serverPort;
    private String serverUsername;
    private String serverPassword;
    private ServerAuthentication serverAuthentication;

    @Configurable(name = "serverHostname")
    public void setServerHostname(String serverHostname) {
        this.serverHostname = serverHostname;
    }

    @Configurable(name = "serverPort")
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    @Configurable(name = "serverUsername", required = false)
    public void setServerUsername(String serverUsername) {
        this.serverUsername = serverUsername;
    }

    @Configurable(name = "serverPassword", required = false)
    public void setServerPassword(String serverPassword) {
        this.serverPassword = serverPassword;
    }

    @Configurable(name = "serverAuthentication", defaultValue = "NONE")
    public void setServerAuthentication(String serverAuthentication) {
        this.serverAuthentication = ServerAuthentication.valueOf(serverAuthentication);
    }

    @PostConfigurationValidator(description = "serverUsername and serverPassword settings are required for the configured serverAuthentication value")
    public boolean validateAuthentication() {
        return ServerAuthentication.NONE.equals(serverAuthentication) || (isNotNullOrBlank(serverUsername) && isNotNullOrBlank(serverPassword));
    }

    public String getServerHostname() {
        return serverHostname;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getServerPassword() {
        return serverPassword;
    }

    public String getServerUsername() {
        return serverUsername;
    }

    public ServerAuthentication getServerAuthentication() {
        return serverAuthentication;
    }

    private boolean isNotNullOrBlank(String s) {
        return s != null && s.trim().length() > 0;
    }

    public enum ServerAuthentication {
        NONE,
        TLS,
        SSL
    }
}
