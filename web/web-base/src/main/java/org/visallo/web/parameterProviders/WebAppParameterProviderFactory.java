package org.visallo.web.parameterProviders;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.parameterProviders.ParameterProvider;
import com.v5analytics.webster.parameterProviders.ParameterProviderFactory;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.WebApp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

public class WebAppParameterProviderFactory extends ParameterProviderFactory<WebApp> {
    private ParameterProvider<WebApp> parameterProvider;

    @Inject
    public WebAppParameterProviderFactory(UserRepository userRepository) {
        parameterProvider = new VisalloBaseParameterProvider<WebApp>(userRepository) {
            @Override
            public WebApp getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
                return getWebApp(request);
            }
        };
    }

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends WebApp> parameterType, Annotation[] parameterAnnotations) {
        return ResourceBundle.class.isAssignableFrom(parameterType);
    }

    @Override
    public ParameterProvider<WebApp> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        return parameterProvider;
    }
}
