package org.visallo.web.parameterProviders;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.parameterProviders.ParameterProvider;
import com.v5analytics.webster.parameterProviders.ParameterProviderFactory;
import org.vertexium.Authorizations;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AuthorizationsParameterProviderFactory extends ParameterProviderFactory<Authorizations> {
    private final ParameterProvider<Authorizations> parameterProvider;

    @Inject
    public AuthorizationsParameterProviderFactory(
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            Configuration configuration,
            AuthorizationRepository authorizationRepository
    ) {
        parameterProvider = new VisalloBaseParameterProvider<Authorizations>(userRepository, configuration) {
            @Override
            public Authorizations getParameter(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    HandlerChain chain
            ) {
                return getAuthorizations(request, getUserRepository(), authorizationRepository, workspaceRepository);
            }
        };
    }

    public static Authorizations getAuthorizations(
            HttpServletRequest request,
            UserRepository userRepository,
            AuthorizationRepository authorizationRepository,
            WorkspaceRepository workspaceRepository
    ) {
        String workspaceId = VisalloBaseParameterProvider.getActiveWorkspaceIdOrDefault(request);
        User user = VisalloBaseParameterProvider.getUser(request, userRepository);
        if (workspaceId != null) {
            if (!workspaceRepository.hasReadPermissions(workspaceId, user)) {
                throw new VisalloAccessDeniedException(
                        "You do not have access to workspace: " + workspaceId,
                        user,
                        workspaceId
                );
            }
            return authorizationRepository.getGraphAuthorizations(user, workspaceId);
        }

        return authorizationRepository.getGraphAuthorizations(user);
    }

    @Override
    public boolean isHandled(
            Method handleMethod,
            Class<? extends Authorizations> parameterType,
            Annotation[] parameterAnnotations
    ) {
        return Authorizations.class.isAssignableFrom(parameterType);
    }

    @Override
    public ParameterProvider<Authorizations> createParameterProvider(
            Method handleMethod,
            Class<?> parameterType,
            Annotation[] parameterAnnotations
    ) {
        return parameterProvider;
    }
}
