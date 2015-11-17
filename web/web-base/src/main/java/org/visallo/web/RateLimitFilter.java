package org.visallo.web;

import com.google.common.util.concurrent.RateLimiter;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.RequestResponseHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RateLimitFilter implements RequestResponseHandler {
    private static final Map<String, RateLimiter> rateLimiters = new HashMap<>();
    public static final int PERMITS_PER_SECOND = 1;
    private static final int TOO_MANY_REQUESTS = 429;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String url = request.getRequestURI();
        RateLimiter rateLimiter = getRateLimiterForUri(url);
        if (rateLimiter.tryAcquire(1, TimeUnit.SECONDS)) {
            chain.next(request, response);
            return;
        }
        response.sendError(TOO_MANY_REQUESTS, "Rate limit reached");
    }

    private RateLimiter getRateLimiterForUri(String url) {
        RateLimiter rateLimiter = rateLimiters.get(url);
        if (rateLimiter == null) {
            rateLimiter = RateLimiter.create(PERMITS_PER_SECOND);
            rateLimiters.put(url, rateLimiter);
        }
        return rateLimiter;
    }
}
