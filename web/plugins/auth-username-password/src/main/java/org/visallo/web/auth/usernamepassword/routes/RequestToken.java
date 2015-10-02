package org.visallo.web.auth.usernamepassword.routes;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BadRequestException;
import org.visallo.web.VisalloResponse;
import org.visallo.web.auth.usernamepassword.ForgotPasswordConfiguration;
import org.visallo.web.auth.usernamepassword.UsernamePasswordWebAppPlugin;
import org.visallo.web.clientapi.model.ClientApiSuccess;
import org.visallo.web.parameterProviders.BaseUrl;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

public class RequestToken implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(RequestToken.class);
    private static final String USERNAME_PARAMETER_NAME = "username";
    private static final String TEMPLATE_PATH = "/org/visallo/web/auth/usernamepassword/templates";
    private static final String TEMPLATE_NAME = "forgotPasswordEmail";
    private static final String CHARSET = "UTF-8";
    private final UserRepository userRepository;
    private ForgotPasswordConfiguration forgotPasswordConfiguration;

    @Inject
    public RequestToken(UserRepository userRepository, Configuration configuration) {
        this.userRepository = userRepository;
        forgotPasswordConfiguration = new ForgotPasswordConfiguration();
        configuration.setConfigurables(forgotPasswordConfiguration, ForgotPasswordConfiguration.CONFIGURATION_PREFIX);
    }

    @Handle
    public ClientApiSuccess handle(
            @BaseUrl String baseUrl,
            @Optional(name = USERNAME_PARAMETER_NAME) String username
    ) throws Exception {
        if (username == null) {
            throw new BadRequestException(USERNAME_PARAMETER_NAME, "username required");
        }

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new BadRequestException(USERNAME_PARAMETER_NAME, "username not found");
        }

        if (user.getEmailAddress() == null) {
            throw new BadRequestException(USERNAME_PARAMETER_NAME, "no e-mail address available for user");
        }

        createTokenAndSendEmail(baseUrl, user);
        return VisalloResponse.SUCCESS;
    }

    private void createTokenAndSendEmail(String baseUrl, User user) throws IOException {
        String token = createToken(user);
        String displayNameOrUsername = user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();
        String url = baseUrl + UsernamePasswordWebAppPlugin.LOOKUP_TOKEN_ROUTE + "?" + LookupToken.TOKEN_PARAMETER_NAME + "=" + token;
        String body = getEmailBody(displayNameOrUsername, url);
        sendEmail(user.getEmailAddress(), body);
        LOGGER.info("sent password reset e-mail to: %s", user.getEmailAddress());
    }

    private String createToken(User user) {
        String token = new BigInteger(240, new SecureRandom()).toString(32);
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.add(Calendar.MINUTE, forgotPasswordConfiguration.getTokenLifetimeMinutes());
        userRepository.setPasswordResetTokenAndExpirationDate(user, token, cal.getTime());
        return token;
    }

    private String getEmailBody(String displayNameOrUsername, String url) throws IOException {
        Map<String, String> context = new HashMap<>();
        context.put("displayNameOrUsername", displayNameOrUsername);
        context.put("url", url);
        TemplateLoader templateLoader = new ClassPathTemplateLoader(TEMPLATE_PATH);
        Handlebars handlebars = new Handlebars(templateLoader);
        Template template = handlebars.compile(TEMPLATE_NAME);
        return template.apply(context);
    }

    private void sendEmail(String to, String body) {
        try {
            MimeMessage mimeMessage = new MimeMessage(getSession());
            //mimeMessage.setHeader("Content-Type", "text/html; charset=" + CHARSET);
            //mimeMessage.setHeader("Content-Transfer-Encoding", "8bit");
            mimeMessage.setFrom(InternetAddress.parse(forgotPasswordConfiguration.getEmailFrom())[0]);
            mimeMessage.setReplyTo(InternetAddress.parse(forgotPasswordConfiguration.getEmailReplyTo()));
            mimeMessage.setSubject(forgotPasswordConfiguration.getEmailSubject(), CHARSET);
            mimeMessage.setText(body, CHARSET);
            mimeMessage.setSentDate(new Date());
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            Transport.send(mimeMessage);
        } catch (MessagingException e) {
            throw new VisalloException("exception while sending e-mail", e);
        }
    }

    private Session getSession() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", forgotPasswordConfiguration.getMailServerHostname());
        properties.put("mail.smtp.port", forgotPasswordConfiguration.getMailServerPort());
        Authenticator authenticator = null;

        switch (forgotPasswordConfiguration.getMailServerAuthentication()) {
            case NONE:
                // no additional properties required
                break;
            case TLS:
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.starttls.enable", "true");
                authenticator = getAuthenticator();
                break;
            case SSL:
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.socketFactory.port", forgotPasswordConfiguration.getMailServerPort());
                properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                authenticator = getAuthenticator();
                break;
            default:
                throw new VisalloException("unexpected MailServerAuthentication: " + forgotPasswordConfiguration.getMailServerAuthentication().toString());
        }

        return Session.getDefaultInstance(properties, authenticator);
    }

    private Authenticator getAuthenticator() {
        return new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(forgotPasswordConfiguration.getMailServerUsername(), forgotPasswordConfiguration.getMailServerPassword());
            }
        };
    }
}
