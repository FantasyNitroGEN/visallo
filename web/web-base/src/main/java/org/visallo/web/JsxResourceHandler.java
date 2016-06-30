package org.visallo.web;


import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.RequestResponseHandler;
import org.apache.commons.io.IOUtils;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.util.js.BabelExecutor;
import org.visallo.web.util.js.CachedCompilation;
import org.visallo.web.util.js.SourceMapType;

import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class JsxResourceHandler implements RequestResponseHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(JsxResourceHandler.class);
    private static final BabelExecutor babelExecutor = new BabelExecutor();

    private String jsResourceName;
    private String jsResourcePath;
    private String toJsResourcePath;
    private SourceMapType sourceMapType;
    private Future<CachedCompilation> compilationTask;
    private volatile CachedCompilation previousCompilation;

    public JsxResourceHandler(final String jsResourceName, final String jsResourcePath, final String toJsResourcePath) {
        this(jsResourceName, jsResourcePath, toJsResourcePath, SourceMapType.INLINE);
    }

    public JsxResourceHandler(final String jsResourceName, final String jsResourcePath, final String toJsResourcePath, SourceMapType sourceMapType) {
        this.jsResourceName = jsResourceName;
        this.jsResourcePath = jsResourcePath;
        this.toJsResourcePath = toJsResourcePath;
        this.sourceMapType = sourceMapType;

        compilationTask = babelExecutor.submit(() -> compileIfNecessary(null));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        CachedCompilation cache = getCache();

        if (request.getRequestURI().endsWith(".map")) {
            write(response, "application/json", cache.getSourceMap());
        } else if (request.getRequestURI().endsWith(".src")) {
            write(response, "application/javascript", cache.getInput());
        } else {
            if (this.sourceMapType == SourceMapType.EXTERNAL && cache.getSourceMap() != null) {
                response.setHeader("X-SourceMap", request.getRequestURI() + ".map");
            }
            write(response, "application/javascript", cache.getOutput());
        }
    }

    private CachedCompilation getCache() throws IOException, InterruptedException, ExecutionException, ScriptException {
        CachedCompilation cache;


        if (compilationTask == null) {
            cache = compileIfNecessary(previousCompilation);
        } else if (compilationTask.isDone()) {
            try {
                previousCompilation = compilationTask.get();
                compilationTask = null;
                cache = compileIfNecessary(previousCompilation);
            } catch (ExecutionException e) {
                cache = compileIfNecessary(previousCompilation);
            }
        } else {
            cache = compilationTask.get();
        }

        previousCompilation = cache;
        return cache;
    }

    private void write(HttpServletResponse response, String contentType, String output) throws IOException {
        if (output != null) {
            try (PrintWriter outWriter = response.getWriter()) {
                response.setContentType(contentType);
                outWriter.println(output);
            }
        } else {
            throw new VisalloException("Errors during compilation: " + jsResourceName);
        }
    }


    private CachedCompilation compileIfNecessary(CachedCompilation previousCompilation) {
        try {
            URL url = this.getClass().getResource(jsResourceName);
            long lastModified = url.openConnection().getLastModified();

            if (previousCompilation == null || previousCompilation.isNecessary(lastModified)) {
                CachedCompilation newCache = new CachedCompilation();
                newCache.setLastModified(lastModified);
                try (InputStream input = this.getClass().getResourceAsStream(jsResourceName)) {
                    try (StringWriter writer = new StringWriter()) {
                        IOUtils.copy(input, writer, StandardCharsets.UTF_8);
                        String inputJavascript = writer.toString();
                        newCache.setInput(inputJavascript);
                        newCache.setPath(toJsResourcePath);
                        newCache.setResourcePath(jsResourceName);
                        babelExecutor.compileWithSharedEngine(newCache, sourceMapType);
                    }
                }
                return newCache;
            }
        } catch (IOException e) {
            throw new VisalloException("Unable to read last modified");
        } catch (ScriptException e) {
            LOGGER.error("%s in file %s", e.getCause().getMessage(), jsResourcePath.replaceAll("^\\/jsc", ""));
            return null;
        }
        return previousCompilation;
    }

}

