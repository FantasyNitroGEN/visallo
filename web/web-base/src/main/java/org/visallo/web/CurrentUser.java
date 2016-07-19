package org.visallo.web;

import org.slf4j.MDC;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class CurrentUser {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(CurrentUser.class);
    public static final String SESSIONUSER_ATTRIBUTE_NAME = "user.current";
    public static final String STRING_ATTRIBUTE_NAME = "username";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_USER_NAME = "userName";
    private static final String MDC_CLIENT_IP_ADDRESS = "clientIpAddress";

    public static void set(HttpServletRequest request, String userId, String userName) {
        request.getSession().setAttribute(CurrentUser.SESSIONUSER_ATTRIBUTE_NAME, new SessionUser(userId));
        request.getSession().setAttribute(CurrentUser.STRING_ATTRIBUTE_NAME, userName);
    }

    public static String getUserId(HttpSession session) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null) {
            LOGGER.debug("sessionUser is null");
            return null;
        }
        return sessionUser.getUserId();
    }

    private static SessionUser getSessionUser(HttpSession session) {
        if (session == null) {
            LOGGER.debug("session is null");
            return null;
        }
        try {
            return (SessionUser) session.getAttribute(CurrentUser.SESSIONUSER_ATTRIBUTE_NAME);
        } catch (IllegalStateException e) {
            LOGGER.debug("Session has expired. Cannot read attributes.");
            return null;
        }
    }

    private static String getSessionUserName(HttpSession session) {
        if (session == null) {
            LOGGER.debug("session is null");
            return null;
        }
        return (String) session.getAttribute(CurrentUser.STRING_ATTRIBUTE_NAME);
    }

    public static String getUserId(HttpServletRequest request) {
        return CurrentUser.getUserId(request.getSession(false));
    }

    public static void clearUserFromSession(HttpServletRequest request) {
        request.getSession().removeAttribute(CurrentUser.SESSIONUSER_ATTRIBUTE_NAME);
        request.getSession().removeAttribute(CurrentUser.STRING_ATTRIBUTE_NAME);
    }

    public static void clearUserFromLogMappedDiagnosticContexts() {
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_USER_NAME);
        MDC.remove(MDC_CLIENT_IP_ADDRESS);
    }

    public static void setUserInLogMappedDiagnosticContexts(HttpServletRequest request) {
        String userId = CurrentUser.getUserId(request);
        if (userId != null) {
            MDC.put(MDC_USER_ID, userId);
        }
        String userName = getSessionUserName(request.getSession(false));
        if (userName != null) {
            MDC.put(MDC_USER_NAME, userName);
        }

        MDC.put(MDC_CLIENT_IP_ADDRESS, getClientIpAddr(request));
    }

    // code adapted from: http://stackoverflow.com/a/15323776/39431
    public static String getClientIpAddr(HttpServletRequest request) {
        String ip;
        ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
