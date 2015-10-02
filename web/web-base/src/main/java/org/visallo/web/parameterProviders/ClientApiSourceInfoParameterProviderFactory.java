package org.visallo.web.parameterProviders;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.parameterProviders.ParameterProvider;
import com.v5analytics.webster.parameterProviders.ParameterProviderFactory;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.clientapi.model.ClientApiSourceInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class ClientApiSourceInfoParameterProviderFactory extends ParameterProviderFactory<ClientApiSourceInfo> {
    private ParameterProvider<ClientApiSourceInfo> parameterProvider;

    @Inject
    public ClientApiSourceInfoParameterProviderFactory(UserRepository userRepository, Configuration configuration) {
        parameterProvider = new VisalloBaseParameterProvider<ClientApiSourceInfo>(userRepository, configuration) {
            @Override
            public ClientApiSourceInfo getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
                final String sourceInfoString = getOptionalParameter(request, "sourceInfo");
                return ClientApiSourceInfo.fromString(sourceInfoString);
            }
        };
    }

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends ClientApiSourceInfo> parameterType, Annotation[] parameterAnnotations) {
        return ClientApiSourceInfo.class.isAssignableFrom(parameterType);
    }

    @Override
    public ParameterProvider<ClientApiSourceInfo> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        return parameterProvider;
    }
}
