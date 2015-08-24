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
import java.util.Locale;
import java.util.ResourceBundle;

public class ResourceBundleParameterProviderFactory extends ParameterProviderFactory<ResourceBundle> {
    private ParameterProvider<ResourceBundle> parameterProvider;

    @Inject
    public ResourceBundleParameterProviderFactory(UserRepository userRepository) {
        parameterProvider = new VisalloBaseParameterProvider<ResourceBundle>(userRepository) {
            @Override
            public ResourceBundle getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
                WebApp webApp = getWebApp(request);
                Locale locale = getLocale(request);
                return webApp.getBundle(locale);
            }
        };
    }

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends ResourceBundle> parameterType, Annotation[] parameterAnnotations) {
        return ResourceBundle.class.isAssignableFrom(parameterType);
    }

    @Override
    public ParameterProvider<ResourceBundle> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        return parameterProvider;
    }
}
