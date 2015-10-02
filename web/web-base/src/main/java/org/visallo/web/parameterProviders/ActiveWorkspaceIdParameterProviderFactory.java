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

import static com.google.common.base.Preconditions.checkNotNull;

public class ActiveWorkspaceIdParameterProviderFactory extends ParameterProviderFactory<String> {
    private final ParameterProvider<String> requiredParameterProvider;
    private final ParameterProvider<String> notRequiredParameterProvider;

    @Inject
    public ActiveWorkspaceIdParameterProviderFactory(UserRepository userRepository, Configuration configuration) {
        requiredParameterProvider = new VisalloBaseParameterProvider<String>(userRepository, configuration) {
            @Override
            public String getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
                return getActiveWorkspaceId(request);
            }
        };
        notRequiredParameterProvider = new VisalloBaseParameterProvider<String>(userRepository, configuration) {
            @Override
            public String getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
                return getActiveWorkspaceIdOrDefault(request);
            }
        };
    }

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends String> parameterType, Annotation[] parameterAnnotations) {
        return getActiveWorkspaceIdAnnotation(parameterAnnotations) != null;
    }

    @Override
    public ParameterProvider<String> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        ActiveWorkspaceId activeWorkspaceIdAnnotation = getActiveWorkspaceIdAnnotation(parameterAnnotations);
        checkNotNull(activeWorkspaceIdAnnotation, "cannot find " + ActiveWorkspaceId.class.getName());
        if (activeWorkspaceIdAnnotation.required()) {
            return requiredParameterProvider;
        } else {
            return notRequiredParameterProvider;
        }
    }

    private static ActiveWorkspaceId getActiveWorkspaceIdAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof ActiveWorkspaceId) {
                return (ActiveWorkspaceId) annotation;
            }
        }
        return null;
    }
}
