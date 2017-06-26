package org.visallo.web.parameterProviders;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.parameterProviders.ParameterProvider;
import com.v5analytics.webster.parameterProviders.ParameterProviderFactory;
import org.visallo.core.config.Configuration;
import org.visallo.core.formula.FormulaEvaluator;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.ResourceBundle;

public class FormulaEvaluatorUserContextParameterProviderFactory extends ParameterProviderFactory<FormulaEvaluator.UserContext> {
    private final ParameterProvider<FormulaEvaluator.UserContext> parameterProvider;

    @Inject
    public FormulaEvaluatorUserContextParameterProviderFactory(
            UserRepository userRepository,
            Configuration configuration,
            WorkspaceRepository workspaceRepository
    ) {
        parameterProvider = new VisalloBaseParameterProvider<FormulaEvaluator.UserContext>(userRepository, configuration) {
            @Override
            public FormulaEvaluator.UserContext getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
                Locale locale = getLocale(request);
                String timeZone = getTimeZone(request);
                ResourceBundle resourceBundle = getBundle(request);
                String workspaceId = getActiveWorkspaceIdOrDefault(request, workspaceRepository, userRepository);
                return new FormulaEvaluator.UserContext(locale, resourceBundle, timeZone, workspaceId);
            }
        };
    }

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends FormulaEvaluator.UserContext> parameterType, Annotation[] parameterAnnotations) {
        return FormulaEvaluator.UserContext.class.isAssignableFrom(parameterType);
    }

    @Override
    public ParameterProvider<FormulaEvaluator.UserContext> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        return parameterProvider;
    }
}
