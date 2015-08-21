package org.visallo.web.parameterProviders;

import com.google.common.base.Preconditions;
import com.v5analytics.webster.parameterProviders.ParameterProvider;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.ProxyUser;
import org.visallo.core.user.User;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.CurrentUser;

import javax.servlet.http.HttpServletRequest;

public abstract class VisalloBaseParameterProvider<T> extends ParameterProvider<T> {
    private static final String VISALLO_WORKSPACE_ID_HEADER_NAME = BaseRequestHandler.VISALLO_WORKSPACE_ID_HEADER_NAME;
    private final UserRepository userRepository;

    public VisalloBaseParameterProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    protected String getWorkspaceIdOrDefault(final HttpServletRequest request) {
        String workspaceId = (String) request.getAttribute("workspaceId");
        if (workspaceId == null || workspaceId.trim().length() == 0) {
            workspaceId = request.getHeader(VISALLO_WORKSPACE_ID_HEADER_NAME);
            if (workspaceId == null || workspaceId.trim().length() == 0) {
                workspaceId = getOptionalParameter(request, "workspaceId");
                if (workspaceId == null || workspaceId.trim().length() == 0) {
                    return null;
                }
            }
        }
        return workspaceId;
    }

    protected String getActiveWorkspaceId(final HttpServletRequest request) {
        String workspaceId = getWorkspaceIdOrDefault(request);
        if (workspaceId == null || workspaceId.trim().length() == 0) {
            throw new VisalloException(VISALLO_WORKSPACE_ID_HEADER_NAME + " is a required header.");
        }
        return workspaceId;
    }

    private String getOptionalParameter(final HttpServletRequest request, final String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");
        return getParameter(request, parameterName, true);
    }

    private String getParameter(final HttpServletRequest request, final String parameterName, final boolean optional) {
        final String paramValue = request.getParameter(parameterName);

        if (paramValue == null) {
            if (!optional) {
                throw new VisalloException(String.format("Parameter: '%s' is required in the request", parameterName));
            }

            return null;
        }

        return paramValue;
    }

    protected User getUser(HttpServletRequest request) {
        ProxyUser user = (ProxyUser) request.getAttribute("user");
        if (user != null) {
            return user;
        }
        user = new ProxyUser(CurrentUser.getUserId(request), getUserRepository());
        request.setAttribute("user", user);
        return user;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }
}
