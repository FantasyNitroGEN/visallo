package org.visallo.web.parameterProviders;

import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.parameterProviders.ParameterProvider;
import com.v5analytics.webster.parameterProviders.ParameterProviderFactory;
import org.visallo.web.VisalloResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class VisalloResponseParameterProviderFactory extends ParameterProviderFactory<VisalloResponse> {
    private static final ParameterProvider<VisalloResponse> PARAMETER_PROVIDER = new ParameterProvider<VisalloResponse>() {
        @Override
        public VisalloResponse getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
            return new VisalloResponse(response);
        }
    };

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends VisalloResponse> parameterType, Annotation[] parameterAnnotations) {
        return VisalloResponse.class.isAssignableFrom(parameterType);
    }

    @Override
    public ParameterProvider<VisalloResponse> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        return PARAMETER_PROVIDER;
    }
}
