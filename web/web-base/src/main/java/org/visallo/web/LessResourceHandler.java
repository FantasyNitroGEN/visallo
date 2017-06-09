package org.visallo.web;

import com.asual.lesscss.LessEngine;
import com.asual.lesscss.LessOptions;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.RequestResponseHandler;
import org.apache.commons.io.IOUtils;
import org.visallo.core.exception.VisalloException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class LessResourceHandler implements RequestResponseHandler {
    private static LessEngine lessCompiler;

    private String lessResourceName;
    private boolean checkLastModified;
    private LessCache cache;

    public LessResourceHandler(String lessResourceName, boolean checkLastModified) {
          this.lessResourceName = lessResourceName;
          this.checkLastModified = checkLastModified;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        response.setContentType("text/css");

        synchronized (lessResourceName.intern()) {
            if (cache == null) {
                cache = new LessCache(getCompiled(), checkLastModified ? getLastModified() : 0l);
            } else if (checkLastModified) {
                long newLastModified = getLastModified();
                if (cache.lastModified != newLastModified) {
                    cache = new LessCache(getCompiled(), newLastModified);
                }
            }
        }

        try (PrintWriter outWriter = response.getWriter()) {
            outWriter.println(cache.getOutput());
        }
    }

    private String getCompiled() throws Exception {
        try (InputStream input = this.getClass().getResourceAsStream(lessResourceName)) {
            try (StringWriter writer = new StringWriter()) {
                IOUtils.copy(input, writer, StandardCharsets.UTF_8);
                String inputLess = writer.toString();
                return lessCompiler().compile(inputLess);
            }
        }
    }

    private long getLastModified() {
        URL url = this.getClass().getResource(lessResourceName);
        try {
            return url.openConnection().getLastModified();
        } catch (IOException e) {
            throw new VisalloException("Unable to find less resource: " + lessResourceName, e);
        }
    }

    private synchronized LessEngine lessCompiler() {
        if (lessCompiler == null) {
            lessCompiler = new LessEngine();
            LessOptions options = new LessOptions();
            options.setCompress(true);
            options.setCharset("UTF-8");
            lessCompiler = new LessEngine(options);
        }
        return lessCompiler;
    }

    class LessCache {
        private long lastModified;
        private String output;

        LessCache(String output, long lastModified) {
            this.lastModified = lastModified;
            this.output = output;
        }

        public long getLastModified() {
            return lastModified;
        }

        public String getOutput() {
            return output;
        }
    }
}
