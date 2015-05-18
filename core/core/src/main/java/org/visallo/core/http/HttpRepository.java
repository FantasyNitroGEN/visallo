package org.visallo.core.http;

import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.commons.io.IOUtils;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Date;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public abstract class HttpRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(HttpRepository.class);
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int DEFAULT_RETRY_COUNT = 2;
    private final Proxy.Type proxyType;
    private final SocketAddress proxyAddress;
    private final String proxyUsername;
    private final String proxyPassword;

    protected HttpRepository(Configuration configuration) {
        String proxyUrlString = configuration.get("http.proxy.url", null);
        Proxy.Type proxyType;
        SocketAddress proxyAddress;
        String proxyUsername;
        String proxyPassword;
        if (proxyUrlString != null) {
            try {
                URL proxyUrl = new URL(proxyUrlString);
                proxyType = Proxy.Type.valueOf(proxyUrl.getProtocol().toUpperCase());
                int port = proxyUrl.getPort();
                if (port == -1) {
                    throw new MalformedURLException("Expected port");
                }
                proxyAddress = new InetSocketAddress(proxyUrl.getHost(), port);
            } catch (MalformedURLException e) {
                throw new VisalloException("Failed to parse url: " + proxyUrlString, e);
            }

            proxyUsername = configuration.get("http.proxy.username", null);
            proxyPassword = configuration.get("http.proxy.password", null);
        } else {
            proxyType = null;
            proxyAddress = null;
            proxyUsername = null;
            proxyPassword = null;
        }
        this.proxyType = proxyType;
        this.proxyAddress = proxyAddress;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
    }

    public byte[] get(String urlString) {
        return get(urlString, DEFAULT_RETRY_COUNT);
    }

    public byte[] get(String urlString, int retryCount) {
        LOGGER.trace("getting %s with retry count %d", urlString, retryCount);
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection;
            if (proxyType != null) {
                Proxy proxy = new Proxy(proxyType, proxyAddress);
                connection = (HttpURLConnection) url.openConnection(proxy);
                if (proxyUsername != null) {
                    String uname_pwd = proxyUsername + ":" + proxyPassword;
                    String authString = "Basic " + new BASE64Encoder().encode(uname_pwd.getBytes());
                    connection.setRequestProperty("Proxy-Authorization", authString);
                }
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.setRequestProperty("Accept-Encoding", "gzip");
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
        } catch (Exception ex) {
            throw new VisalloException("Could not get url: " + urlString, ex);
        }
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
        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (first) {
                first = false;
            } else {
                query.append("&");
            }
            String urlEncodedValue = null;
            try {
                urlEncodedValue = URLEncoder.encode(entry.getValue(), "utf-8");
            } catch (UnsupportedEncodingException e) {
                throw new VisalloException("Could not find encoder", e);
            }
            query.append(entry.getKey()).append("=").append(urlEncodedValue);
        }
        String completeUrl = url + "?" + query.toString();
        return get(completeUrl);
    }
}
