package org.visallo.web.auth.usernamepassword.routes;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.ContentType;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.auth.usernamepassword.ForgotPasswordConfiguration;
import org.visallo.web.auth.usernamepassword.UsernamePasswordWebAppPlugin;
import org.visallo.web.parameterProviders.BaseUrl;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LookupToken implements ParameterizedHandler {
    public static final String TOKEN_PARAMETER_NAME = "token";
    private static final String TEMPLATE_PATH = "/org/visallo/web/auth/usernamepassword/templates";
    private static final String TEMPLATE_NAME = "changePasswordWithToken";
    private final UserRepository userRepository;
    private ForgotPasswordConfiguration forgotPasswordConfiguration;

    @Inject
    public LookupToken(UserRepository userRepository, Configuration configuration) {
        this.userRepository = userRepository;
        forgotPasswordConfiguration = new ForgotPasswordConfiguration();
        configuration.setConfigurables(forgotPasswordConfiguration, ForgotPasswordConfiguration.CONFIGURATION_PREFIX);
    }

    @Handle
    @ContentType("text/html")
    public String handle(
            @BaseUrl String baseUrl,
            @Required(name = TOKEN_PARAMETER_NAME) String token
    ) throws Exception {
        User user = userRepository.findByPasswordResetToken(token);
        if (user == null) {
            throw new VisalloAccessDeniedException("invalid token", null, null);
        }

        Date now = new Date();
        if (!user.getPasswordResetTokenExpirationDate().after(now)) {
            throw new VisalloAccessDeniedException("expired token", user, null);
        }

        return getHtml(baseUrl, token);
    }

    private String getHtml(String baseUrl, String token) throws IOException {
        Map<String, String> context = new HashMap<>();
        context.put("formAction", baseUrl + UsernamePasswordWebAppPlugin.CHANGE_PASSWORD_ROUTE);
        context.put("tokenParameterName", ChangePassword.TOKEN_PARAMETER_NAME);
        context.put("token", token);
        context.put("newPasswordLabel", forgotPasswordConfiguration.getNewPasswordLabel());
        context.put("newPasswordParameterName", ChangePassword.NEW_PASSWORD_PARAMETER_NAME);
        context.put("newPasswordConfirmationLabel", forgotPasswordConfiguration.getNewPasswordConfirmationLabel());
        context.put("newPasswordConfirmationParameterName", ChangePassword.NEW_PASSWORD_CONFIRMATION_PARAMETER_NAME);
        TemplateLoader templateLoader = new ClassPathTemplateLoader(TEMPLATE_PATH);
        Handlebars handlebars = new Handlebars(templateLoader);
        Template template = handlebars.compile(TEMPLATE_NAME);
        return template.apply(context);
    }
}
