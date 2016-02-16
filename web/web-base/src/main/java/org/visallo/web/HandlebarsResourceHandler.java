package org.visallo.web;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.AbstractTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;
import com.github.jknack.handlebars.io.URLTemplateSource;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.RequestResponseHandler;
import org.visallo.core.exception.VisalloException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class HandlebarsResourceHandler implements RequestResponseHandler {
    private final String contentType;
    private final Map<String, Object> properties;
    private final Template template;

    public HandlebarsResourceHandler(Class classRef, String resourcePath, String contentType, Map<String, Object> properties) {
        this.contentType = contentType;
        this.properties = properties;
        TemplateLoader templateLoader = new AbstractTemplateLoader() {
            @Override
            public TemplateSource sourceAt(String location) throws IOException {
                URL resourceUrl = classRef.getResource(location);
                if (resourceUrl == null) {
                    throw new VisalloException("Could not find template: " + resourcePath + " (" + classRef.getName() + ")");
                }
                return new URLTemplateSource(location, resourceUrl);
            }
        };
        Handlebars handlebars = new Handlebars(templateLoader);
        try {
            template = handlebars.compile(resourcePath);
        } catch (IOException e) {
            throw new VisalloException("Could not load template: " + resourcePath + " (" + classRef.getName() + ")", e);
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain handlerChain) throws Exception {
        try (ServletOutputStream out = response.getOutputStream()) {
            response.setContentType(this.contentType);
            String html = template.apply(this.properties);
            out.write(html.getBytes());
        }
    }
}
