package org.visallo.core.http;

import org.apache.commons.io.IOUtils;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public abstract class HttpRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(HttpRepository.class);
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int DEFAULT_RETRY_COUNT = 2;
    private final Proxy.Type proxyType;
    private final SocketAddress proxyAddress;

    protected HttpRepository(Configuration configuration) {
        String proxyUrlString = configuration.get("http.proxy.url", null);
        if (proxyUrlString != null) {
            try {
                URL proxyUrl = new URL(proxyUrlString);
                proxyType = Proxy.Type.valueOf(proxyUrl.getProtocol().toUpperCase());
                int port = proxyUrl.getPort();
                if (port == -1) {
                    throw new MalformedURLException("Expected port");
                }
                proxyAddress = new InetSocketAddress(proxyUrl.getHost(), port);

                String proxyUsername = configuration.get("http.proxy.username", null);
                String proxyPassword = configuration.get("http.proxy.password", null);
                if (proxyUsername != null && proxyPassword != null) {
                    Authenticator.setDefault(new ProxyAuthenticator(proxyUrl.getHost(), proxyUrl.getPort(), proxyUsername, proxyPassword));
                }

                LOGGER.info("configured to use proxy (type: %s, address: %s, username: %s, w/password: %s)", proxyType, proxyAddress, proxyUsername, proxyPassword != null);
            } catch (MalformedURLException e) {
                throw new VisalloException("Failed to parse url: " + proxyUrlString, e);
            }

            if (LOGGER.isTraceEnabled()) {
                String packageName = "sun.net.www.protocol.http";
                LOGGER.trace("configuring java.util.Logging -> Log4J logging for: %s", packageName);
                Handler handler = new Handler() {
                    @Override
                    public void publish(LogRecord record) {
                        LOGGER.trace("%s.%s [%s] %s", record.getSourceClassName(), record.getSourceMethodName(), record.getLevel(), record.getMessage());
                    }

                    @Override
                    public void flush() {
                        // do nothing
                    }

                    @Override
                    public void close() throws SecurityException {
                        // do nothing
                    }
                };
                Logger logger = Logger.getLogger(packageName);
                logger.addHandler(handler);
                logger.setLevel(Level.ALL);
            }
        } else {
            proxyType = null;
            proxyAddress = null;
        }
    }

    public byte[] get(String urlString) {
        return get(urlString, DEFAULT_RETRY_COUNT);
    }

    public byte[] get(String urlString, int retryCount) {
        try {
            HttpURLConnection connection = openConnection("GET", urlString);
            return getResponse(connection, urlString, retryCount);
        } catch (Exception ex) {
            throw new VisalloException("Could not get url: " + urlString, ex);
        }
    }

    private byte[] getResponse(HttpURLConnection connection, String urlString, int retryCount) throws IOException, InterruptedException {
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            LOGGER.warn("Failed to get URL: %s", urlString);
            if (retryCount > 0) {
                if (responseCode == HTTP_TOO_MANY_REQUESTS || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    String rateLimitResetString = connection.getHeaderField("X-RateLimit-Reset");
                    if (rateLimitResetString != null) {
                        long rateLimitReset = Long.parseLong(rateLimitResetString);
                        if (rateLimitReset > 1400000000) {
                            Date resetDate = new Date(rateLimitReset * 1000);
                            long millis = resetDate.getTime() - new Date().getTime();
                            if (millis > 0) {
                                LOGGER.info("Hit rate limit (%s). Waiting until %s or %d seconds.", urlString, resetDate.toString(), millis / 1000);
                                Thread.sleep(millis);
                            } else {
                                LOGGER.info("Hit rate limit (%s). Retrying.", urlString);
                            }
                            return get(urlString, retryCount - 1);
                        } else {
                            LOGGER.info("Hit rate limit (%s). Waiting %d seconds.", urlString, rateLimitReset);
                            Thread.sleep((rateLimitReset + 1) * 1000);
                            return get(urlString, retryCount - 1);
                        }
                    }
                }
            }
            throw new VisalloException(connection.getResponseMessage() + " (" + responseCode + ")");
        }
        return IOUtils.toByteArray(getResponseStream(connection));
    }

    private HttpURLConnection openConnection(String method, String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection;
        if (proxyType != null) {
            Proxy proxy = new Proxy(proxyType, proxyAddress);
            connection = (HttpURLConnection) url.openConnection(proxy);
            LOGGER.debug("%s (via proxy) %s", method, urlString);
        } else {
            connection = (HttpURLConnection) url.openConnection();
            LOGGER.debug("%s %s", method, urlString);
        }
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.setRequestMethod(method);
        return connection;
    }

    public InputStream getResponseStream(URLConnection connection) throws IOException {
        String contentEncoding = connection.getContentEncoding();
        if (contentEncoding != null && contentEncoding.toLowerCase().contains("gzip")) {
            return new GZIPInputStream(connection.getInputStream());
        }

        // Detect Gzipped content
        byte[] magicNumber = new byte[2];
        PushbackInputStream pushbackInputStream = new PushbackInputStream(connection.getInputStream(), magicNumber.length);
        int read = pushbackInputStream.read(magicNumber);
        pushbackInputStream.unread(magicNumber, 0, read);
        if (read >= magicNumber.length) {
            if ((byte) ((GZIPInputStream.GZIP_MAGIC & 0xff00) >> 8) == magicNumber[1]
                    && (byte) (GZIPInputStream.GZIP_MAGIC & 0xff) == magicNumber[0]) {
                return new GZIPInputStream(pushbackInputStream);
            }
        }

        return pushbackInputStream;
    }

    public byte[] get(String url, Map<String, String> parameters) {
        String completeUrl = createUrl(url, Parameter.toList(parameters));
        return get(completeUrl);
    }

    public byte[] post(String url, Map<String, String> urlParameters, List<Parameter> formParameters) {
        String completeUrl = createUrl(url, Parameter.toList(urlParameters));
        return post(completeUrl, formParameters);
    }

    public byte[] post(String urlString, List<Parameter> formParameters) {
        try {
            byte[] formData = createQueryString(formParameters).getBytes();
            HttpURLConnection connection = openConnection("POST", urlString);
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Length", Integer.toString(formData.length));
            connection.setUseCaches(false);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(formData);
            }
            return getResponse(connection, urlString, 0);
        } catch (Exception ex) {
            throw new VisalloException("Could not post url: " + urlString, ex);
        }
    }

    private String createUrl(String url, List<Parameter> parameters) {
        String queryString = createQueryString(parameters);
        return url + "?" + queryString;
    }

    protected String createQueryString(List<Parameter> parameters) {
        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (Parameter entry : parameters) {
            if (first) {
                first = false;
            } else {
                query.append("&");
            }
            String urlEncodedValue;
            try {
                urlEncodedValue = URLEncoder.encode(entry.getValue(), "utf-8");
            } catch (UnsupportedEncodingException e) {
                throw new VisalloException("Could not find encoder", e);
            }
            query.append(entry.getName()).append("=").append(urlEncodedValue);
        }
        return query.toString();
    }

    private class ProxyAuthenticator extends Authenticator {
        private String proxyHost;
        private int proxyPort;
        private String username;
        private char[] password;

        public ProxyAuthenticator(String proxyHost, int proxyPort, String username, String password) {
            this.proxyHost = proxyHost;
            this.proxyPort = proxyPort;
            this.username = username;
            this.password = password.toCharArray();
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            if (getRequestingHost().equals(proxyHost) && getRequestingPort() == proxyPort) {
                LOGGER.trace("ProxyAuthenticator.getPasswordAuthentication() Responding to proxy authentication request");
                return new PasswordAuthentication(username, password);
            }
            LOGGER.trace("ProxyAuthenticator.getPasswordAuthentication() Ignoring authentication request for: %s:%d", getRequestingHost(), getRequestingPort());
            return null;
        }
    }

    public static class Parameter {
        private final String name;
        private final String value;

        public Parameter(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public static List<Parameter> toList(Map<String, String> parameters) {
            List<Parameter> results = new ArrayList<>();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                results.add(new Parameter(entry.getKey(), entry.getValue()));
            }
            return results;
        }
    }
}
