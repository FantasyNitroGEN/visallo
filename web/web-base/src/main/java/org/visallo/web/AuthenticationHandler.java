package org.visallo.web;

import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.RequestResponseHandler;
import org.visallo.web.util.RemoteAddressUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthenticationHandler implements RequestResponseHandler {
    public static final String LOGIN_PATH = "/login";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String currentUserId = CurrentUser.getUserId(request);
        if (currentUserId != null) {
            chain.next(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    /**
     * @Deprecated
     *
     * Use RemoteAddressUtil.getClientIpAddr for future calls to get client IP addresses.
     */
    @Deprecated
    public static String getRemoteAddr(HttpServletRequest request) {
        return RemoteAddressUtil.getClientIpAddr(request);
    }
}
