package org.visallo.web.parameterProviders;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.parameterProviders.ParameterProvider;
import com.v5analytics.webster.parameterProviders.ParameterProviderFactory;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.WebConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class JustificationTextParameterProviderFactory extends ParameterProviderFactory<String> {
    public static final String JUSTIFICATION_TEXT = "justificationText";
    private final ParameterProvider<String> parameterProvider;

    @Inject
    public JustificationTextParameterProviderFactory(UserRepository userRepository, final Configuration configuration) {
        final boolean isJustificationRequired = WebConfiguration.justificationRequired(configuration);

        parameterProvider = new VisalloBaseParameterProvider<String>(userRepository, configuration) {
            @Override
            public String getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
                String propertyName = getOptionalParameter(request, "propertyName");
                if (propertyName != null && propertyName.length() > 0) {
                    boolean isComment = VisalloProperties.COMMENT.getPropertyName().equals(propertyName);
                    String sourceInfo = getOptionalParameter(request, "sourceInfo");
                    return getJustificationText(isComment, sourceInfo, request);
                } else {
                    return justificationParameter(isJustificationRequired, request);
                }
            }

            public String getJustificationText(boolean isComment, String sourceInfo, HttpServletRequest request) {
                return justificationParameter(isJustificationRequired(isComment, sourceInfo), request);
            }

            public boolean isJustificationRequired(boolean isComment, String sourceInfo) {
                return !isComment && sourceInfo == null && isJustificationRequired;
            }

            private String justificationParameter(boolean required, HttpServletRequest request) {
                return required ?
                        getRequiredParameter(request, JUSTIFICATION_TEXT) :
                        getOptionalParameter(request, JUSTIFICATION_TEXT);
            }
        };
    }

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends String> parameterType, Annotation[] parameterAnnotations) {
        return getJustificationTextAnnotation(parameterAnnotations) != null;
    }

    @Override
    public ParameterProvider<String> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        return parameterProvider;
    }

    private static JustificationText getJustificationTextAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof JustificationText) {
                return (JustificationText) annotation;
            }
        }
        return null;
    }
}
