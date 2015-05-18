package org.visallo.web;

import com.v5analytics.webster.Handler;

import javax.servlet.ServletContext;

public interface WebAppPlugin {
    void init(WebApp app, ServletContext servletContext, Handler authenticationHandler);
}
