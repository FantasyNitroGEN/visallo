package org.visallo.web;

import org.mortbay.servlet.GzipFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class ApplicationGzipFilter extends GzipFilter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String path = ((HttpServletRequest) request).getRequestURI();
        if (path.startsWith(Messaging.PATH)) {
            chain.doFilter(request, response);
        } else {
            super.doFilter(request, response, chain);
        }
    }
}
