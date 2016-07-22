package org.visallo.web.parameterProviders;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.v5analytics.webster.App;
import com.v5analytics.webster.parameterProviders.ParameterProvider;
import org.vertexium.FetchHint;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.ProxyUser;
import org.visallo.core.user.User;
import org.visallo.web.CurrentUser;
import org.visallo.web.WebApp;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.TimeZone;

public abstract class VisalloBaseParameterProvider<T> extends ParameterProvider<T> {
    public static final String VISALLO_WORKSPACE_ID_HEADER_NAME = "Visallo-Workspace-Id";
    public static final String VISALLO_SOURCE_GUID_HEADER_NAME = "Visallo-Source-Guid";
    private static final String LOCALE_LANGUAGE_PARAMETER = "localeLanguage";
    private static final String LOCALE_COUNTRY_PARAMETER = "localeCountry";
    private static final String LOCALE_VARIANT_PARAMETER = "localeVariant";
    private static final String VISALLO_TIME_ZONE_HEADER_NAME = "Visallo-TimeZone";
    private static final String TIME_ZONE_ATTRIBUTE_NAME = "timeZone";
    private static final String TIME_ZONE_PARAMETER_NAME = "timeZone";
    private final UserRepository userRepository;
    private final Configuration configuration;

    public VisalloBaseParameterProvider(UserRepository userRepository, Configuration configuration) {
        this.userRepository = userRepository;
        this.configuration = configuration;
    }

    protected static String getActiveWorkspaceIdOrDefault(final HttpServletRequest request) {
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

    protected static String getActiveWorkspaceId(final HttpServletRequest request) {
        String workspaceId = getActiveWorkspaceIdOrDefault(request);
        if (workspaceId == null || workspaceId.trim().length() == 0) {
            throw new VisalloException(VISALLO_WORKSPACE_ID_HEADER_NAME + " is a required header.");
        }
        return workspaceId;
    }

    protected static String getSourceGuid(final HttpServletRequest request) {
        return request.getHeader(VISALLO_SOURCE_GUID_HEADER_NAME);
    }

    public static String getOptionalParameter(final HttpServletRequest request, final String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");
        return getParameter(request, parameterName, true);
    }

    public static String[] getOptionalParameterArray(HttpServletRequest request, String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");

        return getParameterValues(request, parameterName, true);
    }

    public static EnumSet<FetchHint> getOptionalParameterFetchHints(
            HttpServletRequest request,
            String parameterName,
            EnumSet<FetchHint> defaultFetchHints
    ) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null) {
            return defaultFetchHints;
        }
        return EnumSet.copyOf(Lists.transform(Arrays.asList(val.split(",")), new Function<String, FetchHint>() {
            @Override
            public FetchHint apply(String input) {
                return FetchHint.valueOf(input);
            }
        }));
    }

    public static Integer getOptionalParameterInt(
            final HttpServletRequest request,
            final String parameterName,
            Integer defaultValue
    ) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null || val.length() == 0) {
            return defaultValue;
        }
        return Integer.parseInt(val);
    }

    public static String[] getOptionalParameterAsStringArray(
            final HttpServletRequest request,
            final String parameterName
    ) {
        Preconditions.checkNotNull(request, "The provided request was invalid");
        return getParameterValues(request, parameterName, true);
    }

    public static Float getOptionalParameterFloat(
            final HttpServletRequest request,
            final String parameterName,
            Float defaultValue
    ) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null || val.length() == 0) {
            return defaultValue;
        }
        return Float.parseFloat(val);
    }

    public static Double getOptionalParameterDouble(
            final HttpServletRequest request,
            final String parameterName,
            Double defaultValue
    ) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null || val.length() == 0) {
            return defaultValue;
        }
        return Double.parseDouble(val);
    }

    protected static String[] getParameterValues(
            final HttpServletRequest request,
            final String parameterName,
            final boolean optional
    ) {
        String[] paramValues = request.getParameterValues(parameterName);

        if (paramValues == null) {
            Object value = request.getAttribute(parameterName);
            if (value instanceof String[]) {
                paramValues = (String[]) value;
            }
        }

        if (paramValues == null) {
            if (!optional) {
                throw new RuntimeException(String.format("Parameter: '%s' is required in the request", parameterName));
            }
            return null;
        }

        return paramValues;
    }

    public static String[] getRequiredParameterArray(HttpServletRequest request, String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");
        return getParameterValues(request, parameterName, false);
    }

    public static String getRequiredParameter(final HttpServletRequest request, final String parameterName) {
        String result = getOptionalParameter(request, parameterName);
        if (result == null) {
            throw new VisalloException("parameter " + parameterName + " is required");
        }
        return result;
    }

    protected static String getParameter(
            final HttpServletRequest request,
            final String parameterName,
            final boolean optional
    ) {
        String paramValue = request.getParameter(parameterName);
        if (paramValue == null) {
            Object paramValueObject = request.getAttribute(parameterName);
            if (paramValueObject != null) {
                paramValue = paramValueObject.toString();
            }
            if (paramValue == null) {
                if (!optional) {
                    throw new VisalloException(String.format(
                            "Parameter: '%s' is required in the request",
                            parameterName
                    ));
                }
                return null;
            }
        }
        return paramValue;
    }

    protected User getUser(HttpServletRequest request) {
        return getUser(request, getUserRepository());
    }

    public static User getUser(
            HttpServletRequest request,
            UserRepository userRepository
    ) {
        ProxyUser user = (ProxyUser) request.getAttribute("user");
        if (user != null) {
            return user;
        }
        user = new ProxyUser(CurrentUser.getUserId(request), userRepository);
        request.setAttribute("user", user);
        return user;
    }

    protected WebApp getWebApp(HttpServletRequest request) {
        return (WebApp) App.getApp(request);
    }

    protected Locale getLocale(HttpServletRequest request) {
        String language = getOptionalParameter(request, LOCALE_LANGUAGE_PARAMETER);
        String country = getOptionalParameter(request, LOCALE_COUNTRY_PARAMETER);
        String variant = getOptionalParameter(request, LOCALE_VARIANT_PARAMETER);

        if (language != null) {
            return WebApp.getLocal(language, country, variant);
        }
        return request.getLocale();
    }

    protected String getTimeZone(final HttpServletRequest request) {
        String timeZone = (String) request.getAttribute(TIME_ZONE_ATTRIBUTE_NAME);
        if (timeZone == null || timeZone.trim().length() == 0) {
            timeZone = request.getHeader(VISALLO_TIME_ZONE_HEADER_NAME);
            if (timeZone == null || timeZone.trim().length() == 0) {
                timeZone = getOptionalParameter(request, TIME_ZONE_PARAMETER_NAME);
                if (timeZone == null || timeZone.trim().length() == 0) {
                    timeZone = this.configuration.get(
                            Configuration.DEFAULT_TIME_ZONE,
                            TimeZone.getDefault().getDisplayName()
                    );
                }
            }
        }
        return timeZone;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }
}
