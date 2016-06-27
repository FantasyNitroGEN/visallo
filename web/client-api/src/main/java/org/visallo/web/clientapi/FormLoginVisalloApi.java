package org.visallo.web.clientapi;


import org.visallo.web.clientapi.model.ClientApiWorkspace;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

public abstract class FormLoginVisalloApi extends VisalloApi {

    abstract String getLoginFormBody();

    public FormLoginVisalloApi(String basePath) {
        this(basePath, false);
    }

    public FormLoginVisalloApi(String basePath, boolean ignoreSslErrors) {
        super(basePath);

        if (ignoreSslErrors) {
            try {
                javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
                    public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                        return true;
                    }
                });

                TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }};

                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception ex) {
                throw new VisalloClientApiException("Could not ignore SSL errors", ex);
            }
        }
    }

    protected ClientApiWorkspace logIn() {
        try {
            URL url = new URL(getBasePath() + "/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setDoOutput(true);

            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(getLoginFormBody());
            out.flush();
            out.close();

            int code = conn.getResponseCode();
            if (code != 200) {
                throw new VisalloClientApiException("Invalid response code. Expected 200. Found " + code);
            }
            Map<String, List<String>> responseHeaders = conn.getHeaderFields();
            List<String> cookies = responseHeaders.get("Set-Cookie");
            if (cookies == null) {
                throw new VisalloClientApiException("Could not find cookie header in response");
            }
            for (String cookie : cookies) {
                if (!cookie.startsWith("JSESSIONID=")) {
                    continue;
                }
                String cookieValue = cookie.substring("JSESSIONID=".length());
                int sep = cookieValue.indexOf(';');
                if (sep > 0) {
                    cookieValue = cookieValue.substring(0, sep);
                }
                ApiInvoker.getInstance().setJSessionId(cookieValue);

                return loginAndGetCurrentWorkspace();
            }
            throw new VisalloClientApiException("Could not find JSESSIONID cookie");
        } catch (Exception e) {
            throw new VisalloClientApiException("Could not login: " + getBasePath(), e);
        }
    }
}
