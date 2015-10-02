package org.visallo.web.parameterProviders;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.parameterProviders.ParameterProvider;
import com.v5analytics.webster.parameterProviders.ParameterProviderFactory;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class BaseUrlParameterProviderFactory extends ParameterProviderFactory<String> {
    private final ParameterProvider<String> parameterProvider;

    @Inject
    public BaseUrlParameterProviderFactory(UserRepository userRepository, final Configuration configuration) {
        parameterProvider = new VisalloBaseParameterProvider<String>(userRepository, configuration) {
            @Override
            public String getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
                String configuredBaseUrl = configuration.get(Configuration.BASE_URL, null);
                if (configuredBaseUrl != null && configuredBaseUrl.trim().length() > 0) {
                    return configuredBaseUrl;
                }

                String scheme = request.getScheme();
                String serverName = request.getServerName();
                int port = request.getServerPort();
                String contextPath = request.getContextPath();

                StringBuilder sb = new StringBuilder();
                sb.append(scheme).append("://").append(serverName);
                if (!(scheme.equals("http") && port == 80 || scheme.equals("https") && port == 443)) {
                    sb.append(":").append(port);
                }
                sb.append(contextPath);
                return sb.toString();
            }
        };
    }

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends String> parameterType, Annotation[] parameterAnnotations) {
        return getBaseUrlAnnotation(parameterAnnotations) != null;
    }

    @Override
    public ParameterProvider<String> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        return parameterProvider;
    }

    private static BaseUrl getBaseUrlAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof BaseUrl) {
                return (BaseUrl) annotation;
            }
        }
        return null;
    }
}
