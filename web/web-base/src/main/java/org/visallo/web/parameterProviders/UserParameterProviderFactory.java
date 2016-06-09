package org.visallo.web.parameterProviders;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.parameterProviders.ParameterProvider;
import com.v5analytics.webster.parameterProviders.ParameterProviderFactory;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class UserParameterProviderFactory extends ParameterProviderFactory<User> {
    private final ParameterProvider<User> parameterProvider;

    @Inject
    public UserParameterProviderFactory(UserRepository userRepository, Configuration configuration) {
        parameterProvider = new VisalloBaseParameterProvider<User>(
                userRepository,
                configuration
        ) {
            @Override
            public User getParameter(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    HandlerChain chain
            ) {
                return getUser(request);
            }
        };
    }

    @Override
    public boolean isHandled(
            Method handleMethod,
            Class<? extends User> parameterType,
            Annotation[] parameterAnnotations
    ) {
        return User.class.isAssignableFrom(parameterType);
    }

    @Override
    public ParameterProvider<User> createParameterProvider(
            Method handleMethod,
            Class<?> parameterType,
            Annotation[] parameterAnnotations
    ) {
        return parameterProvider;
    }
}
