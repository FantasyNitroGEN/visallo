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
        return (SessionUser) session.getAttribute(CurrentUser.SESSIONUSER_ATTRIBUTE_NAME);
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
    }
}
