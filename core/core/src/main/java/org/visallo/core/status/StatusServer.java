package org.visallo.core.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.status.model.ProcessStatus;
import org.visallo.core.status.model.Status;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public abstract class StatusServer {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(StatusServer.class);
    private final Configuration configuration;
    private final HttpServer httpServer;
    private final Date startTime;
    private final String type;
    private final Class sourceClass;
    private final StatusRepository.StatusHandle statusHandle;
    private final StatusRepository statusRepository;

    public StatusServer(Configuration configuration, StatusRepository statusRepository, String type, Class sourceClass) {
        this.statusRepository = statusRepository;
        this.sourceClass = sourceClass;
        this.type = type;
        this.configuration = configuration;
        this.startTime = new Date();

        String portRange = configuration.get(Configuration.STATUS_PORT_RANGE, Configuration.DEFAULT_STATUS_PORT_RANGE);
        httpServer = startHttpServer(portRange);

        String url = getUrl();
        LOGGER.debug("Using url: " + url);
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            StatusData statusData = new StatusData(url, hostname, hostAddress);
            this.statusHandle = statusRepository.saveStatus(type, UUID.randomUUID().toString(), statusData);
        } catch (UnknownHostException e) {
            throw new VisalloException("Could not get local host address", e);
        }
    }

    private String getUrl() {
        try {
            String hostname = InetAddress.getLocalHost().getHostAddress();
            return String.format("http://%s:%d/", hostname, httpServer.getAddress().getPort());
        } catch (UnknownHostException ex) {
            throw new VisalloException("Could not create url", ex);
        }
    }

    private HttpServer startHttpServer(String portRange) {
        String[] parts = portRange.split("-");
        if (parts.length != 2) {
            throw new VisalloException("Invalid port range: " + portRange);
        }
        int startPort = Integer.parseInt(parts[0]);
        int endPort = Integer.parseInt(parts[1]);
        return startHttpServer(startPort, endPort);
    }

    private HttpServer startHttpServer(int startPort, int endPort) {
        for (int i = startPort; i < endPort; i++) {
            try {
                HttpServer httpServer = HttpServer.create(new InetSocketAddress(i), 0);
                httpServer.createContext("/", new StatusHandler());
                httpServer.setExecutor(null);
                httpServer.start();
                LOGGER.info("Started status HTTP server on port: %s", i);
                return httpServer;
            } catch (BindException ex) {
                LOGGER.debug("Could not start HTTP server on port %d", i);
            } catch (Throwable ex) {
                LOGGER.debug("Could not start HTTP server on port %d", i, ex);
            }
        }
        throw new VisalloException("Could not start HTTP status server");
    }

    public void shutdown() {
        try {
            httpServer.stop(0);
        } catch (Throwable ex) {
            LOGGER.error("Could not stop http server", ex);
        }

        statusRepository.deleteStatus(this.statusHandle);
    }

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                Status status = getStatus();
                String responseString = ClientApiConverter.clientApiToString(status);
                t.sendResponseHeaders(200, 0);
                OutputStream os = t.getResponseBody();
                os.write(responseString.getBytes());
                os.close();
            } catch (Throwable ex) {
                LOGGER.error("Could not process request", ex);
            }
        }
    }

    private Status getStatus() {
        ProcessStatus status = createStatus();
        getGeneralInfo(status, this.sourceClass);
        status.setType(type);
        status.setStartTime(startTime);
        try {
            status.setHostname(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            LOGGER.error("Could not get hostname");
        }
        status.setOsUser(System.getProperty("user.name"));
        for (Map.Entry<String, String> envEntry : System.getenv().entrySet()) {
            status.getEnv().put(envEntry.getKey(), envEntry.getValue());
        }
        status.setConfiguration(ClientApiConverter.toClientApiValue(getConfigurationJson()));
        status.getJvm().setClasspath(System.getProperty("java.class.path"));
        return status;
    }

    protected abstract ProcessStatus createStatus();

    private JSONObject getConfigurationJson() {
        JSONObject json = new JSONObject();
        json.put("properties", configuration.getJsonProperties());
        json.put("configurationInfo", configuration.getConfigurationInfo());
        return json;
    }

    private static Manifest getManifest(Class clazz) {
        try {
            String className = clazz.getSimpleName() + ".class";
            URL resource = clazz.getResource(className);
            if (resource == null) {
                LOGGER.error("Could not get class manifest: " + clazz.getName() + ", could not find resource: " + className);
                return null;
            }
            String classPath = resource.toString();
            if (!classPath.startsWith("jar")) {
                return null; // Class not from JAR
            }
            String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
            return new Manifest(new URL(manifestPath).openStream());
        } catch (Exception ex) {
            LOGGER.error("Could not get class manifest: " + clazz.getName(), ex);
            return null;
        }
    }

    public static void getGeneralInfo(JSONObject json, Class clazz) {
        json.put("className", clazz.getName());

        Name nameAnnotation = (Name) clazz.getAnnotation(Name.class);
        if (nameAnnotation != null) {
            json.put("name", nameAnnotation.value());
        }

        Description descriptionAnnotation = (Description) clazz.getAnnotation(Description.class);
        if (descriptionAnnotation != null) {
            json.put("description", descriptionAnnotation.value());
        }

        Manifest manifest = getManifest(clazz);
        if (manifest != null) {
            Attributes mainAttributes = manifest.getMainAttributes();
            json.put("projectVersion", mainAttributes.getValue("Project-Version"));
            json.put("gitRevision", mainAttributes.getValue("Git-Revision"));
            json.put("builtBy", mainAttributes.getValue("Built-By"));
            String value = mainAttributes.getValue("Built-On-Unix");
            if (value != null) {
                json.put("builtOn", Long.parseLong(value));
            }
        }
    }

    public static void getGeneralInfo(Status generalStatus, Class clazz) {
        generalStatus.setClassName(clazz.getName());

        Name nameAnnotation = (Name) clazz.getAnnotation(Name.class);
        if (nameAnnotation != null) {
            generalStatus.setName(nameAnnotation.value());
        }

        Description descriptionAnnotation = (Description) clazz.getAnnotation(Description.class);
        if (descriptionAnnotation != null) {
            generalStatus.setDescription(descriptionAnnotation.value());
        }

        Manifest manifest = getManifest(clazz);
        if (manifest != null) {
            Attributes mainAttributes = manifest.getMainAttributes();
            generalStatus.setProjectVersion(mainAttributes.getValue("Project-Version"));
            generalStatus.setGitRevision(mainAttributes.getValue("Git-Revision"));
            generalStatus.setBuiltBy(mainAttributes.getValue("Built-By"));
            String value = mainAttributes.getValue("Built-On-Unix");
            if (value != null) {
                generalStatus.setBuiltOn(new Date(Long.parseLong(value)));
            }
        }
    }
}
