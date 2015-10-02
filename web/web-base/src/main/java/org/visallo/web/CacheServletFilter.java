package org.visallo.web;

import com.google.inject.Inject;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CacheServletFilter implements Filter {
    private Configuration configuration;
    private Integer maxAge;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        InjectHelper.inject(this);
        String maxAgeString = this.configuration.get("web.cacheServletFilter.maxAge", null);
        if (maxAgeString != null) {
            maxAge = Integer.parseInt(maxAgeString);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (response instanceof HttpServletResponse && maxAge != null) {
            ((HttpServletResponse) response).setHeader("Cache-Control", "max-age=" + maxAge);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
