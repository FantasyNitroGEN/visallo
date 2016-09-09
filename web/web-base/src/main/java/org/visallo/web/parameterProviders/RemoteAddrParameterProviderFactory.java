package org.visallo.web.parameterProviders;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.parameterProviders.ParameterProvider;
import com.v5analytics.webster.parameterProviders.ParameterProviderFactory;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.util.RemoteAddressUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

public class RemoteAddrParameterProviderFactory extends ParameterProviderFactory<String> {
    private final ParameterProvider<String> parameterProvider;

    @Inject
    public RemoteAddrParameterProviderFactory(UserRepository userRepository, Configuration configuration) {
        parameterProvider = new VisalloBaseParameterProvider<String>(userRepository, configuration) {
            @Override
            public String getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
                return RemoteAddressUtil.getClientIpAddr(request);
            }
        };
    }

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends String> parameterType, Annotation[] parameterAnnotations) {
        return getRemoteAddrAnnotation(parameterAnnotations) != null;
    }

    @Override
    public ParameterProvider<String> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        RemoteAddr remoteAddrAnnotation = getRemoteAddrAnnotation(parameterAnnotations);
        checkNotNull(remoteAddrAnnotation, "cannot find " + RemoteAddr.class.getName());
        return parameterProvider;
    }

    private static RemoteAddr getRemoteAddrAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof RemoteAddr) {
                return (RemoteAddr) annotation;
            }
        }
        return null;
    }
}
