package org.visallo.web;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.*;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.RequestResponseHandler;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HandlebarsPrecompiledResourceHandler implements RequestResponseHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(HandlebarsPrecompiledResourceHandler.class);

    private final String path;
    private String cached;
    private Long cachedLastModified;
    private boolean recompileOnModification;
    
    public HandlebarsPrecompiledResourceHandler(String path, boolean recompileOnModification) {
        this.path = path;
        this.recompileOnModification = recompileOnModification;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain handlerChain) throws Exception {
        try (ServletOutputStream out = response.getOutputStream()) {
            response.setContentType("application/javascript");

            if (shouldRecompile()) {
                if (LOGGER.isDebugEnabled()) {
                    long start = System.nanoTime();
                    compile();
                    LOGGER.debug("Compiled template %s (%d ms)", path, TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS));
                } else {
                    compile();
                }
            }

            out.write(cached.getBytes());
        }
    }

    private boolean shouldRecompile() throws IOException {
        if (cached == null) {
            return true;
        }

        if (recompileOnModification) {
            return (cachedLastModified == null || cachedLastModified != getLastModified());
        }

        return false;
    }

    private long getLastModified() {
        URL url = this.getClass().getResource(path);
        try {
            return url.openConnection().getLastModified();
        } catch (IOException e) {
            throw new VisalloException("Unable to find template file: " + path, e);
        }
    }

    private void compile() {
        Handlebars handlebars = new Handlebars(new ClassPathTemplateLoader("", ""));
        try {
            Template template = handlebars.compile(path);
            String precompiled = template.toJavaScript();
            StringBuffer buffer = new StringBuffer()
                .append("define(['handlebars'], function(Handlebars) {\n")
                .append("    return Handlebars.template(")
                                .append(precompiled)
                .append("    );\n")
                .append("});");

            Handlebars.SafeString output = new Handlebars.SafeString(buffer);
            cached = output.toString();
            if (recompileOnModification) {
                cachedLastModified = getLastModified();
            }
        } catch (IOException e) {
            throw new VisalloException("Unable to precompile template: " + path, e);
        }
    }
}
