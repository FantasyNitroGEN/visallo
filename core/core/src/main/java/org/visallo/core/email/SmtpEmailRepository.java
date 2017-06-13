package org.visallo.core.email;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;

public class SmtpEmailRepository implements EmailRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(SmtpEmailRepository.class);
    private static final String CHARSET = "UTF-8";
    private SmtpEmailConfiguration smtpEmailConfiguration;

    @Inject
    public SmtpEmailRepository(Configuration configuration) {
        smtpEmailConfiguration = new SmtpEmailConfiguration();
        configuration.setConfigurables(smtpEmailConfiguration, SmtpEmailConfiguration.CONFIGURATION_PREFIX);
    }

    @Override
    public void send(String fromAddress, String toAddress, String subject, String body) {
        send(fromAddress, new String[]{toAddress}, subject, body);
    }

    @Override
    public void send(String fromAddress, String[] toAddresses, String subject, String body) {
        String joinedToAddresses = Joiner.on(",").join(toAddresses);
        LOGGER.info("sending SMTP email from: \"%s\", to: \"%s\", subject: \"%s\"", fromAddress, joinedToAddresses, subject);
        LOGGER.debug("sending SMTP email body:%n%s", body);

        try {
            MimeMessage mimeMessage = new MimeMessage(getSession());
            mimeMessage.setFrom(InternetAddress.parse(fromAddress)[0]);
            mimeMessage.setSubject(subject, CHARSET);
            if (body.startsWith("<html>")) {
                Multipart multipart = new MimeMultipart();
                MimeBodyPart html = new MimeBodyPart();
                String contentType = "text/html; charset=" + CHARSET;
                html.setHeader("Content-Type", contentType);
                html.setContent(body, contentType);
                multipart.addBodyPart(html);
                mimeMessage.setContent(multipart);
            } else {
                mimeMessage.setText(body, CHARSET);
            }
            mimeMessage.setSentDate(new Date());
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(joinedToAddresses));
            Transport.send(mimeMessage);
        } catch (MessagingException me) {
            throw new VisalloException("exception while sending email", me);
        }
    }

    private Session getSession() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", smtpEmailConfiguration.getServerHostname());
        properties.put("mail.smtp.port", smtpEmailConfiguration.getServerPort());
        Authenticator authenticator = null;

        switch (smtpEmailConfiguration.getServerAuthentication()) {
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
                properties.put("mail.smtp.socketFactory.port", smtpEmailConfiguration.getServerPort());
                properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                authenticator = getAuthenticator();
                break;
            default:
                throw new VisalloException("unexpected MailServerAuthentication: " + smtpEmailConfiguration.getServerAuthentication().toString());
        }

        Session session = Session.getDefaultInstance(properties, authenticator);
        if (LOGGER.isTraceEnabled()) {
            session.setDebugOut(new LoggerPrintStream(LOGGER));
            session.setDebug(true);
        }
        return session;
    }

    private Authenticator getAuthenticator() {
        return new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpEmailConfiguration.getServerUsername(), smtpEmailConfiguration.getServerPassword());
            }
        };
    }

    private class LoggerPrintStream extends PrintStream {
        public LoggerPrintStream(final VisalloLogger logger) {
            super(new OutputStream() {
                private final int NEWLINE = "\n".getBytes()[0];
                private final StringWriter buffer = new StringWriter();

                @Override
                public void write(int c) throws IOException {
                    if (c == NEWLINE) {
                        logger.trace(buffer.toString());
                        buffer.getBuffer().setLength(0);
                    } else {
                        buffer.write(c);
                    }
                }
            });
        }
    }
}
