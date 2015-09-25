package org.visallo.core.action;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.visallo.core.email.EmailRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public class EmailAction extends Action {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(EmailAction.class);
    public static final String PROPERTY_FROM_ADDRESS = "fromAddress";
    public static final String PROPERTY_TO_ADDRESS = "toAddress";
    public static final String PROPERTY_SUBJECT = "subject";
    public static final String PROPERTY_BODY = "body";
    private final EmailRepository emailRepository;

    @Inject
    public EmailAction(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    @Override
    public void validateData(JSONObject actionData) {
        super.validateData(actionData);
        validateDataHas(actionData, PROPERTY_FROM_ADDRESS);
        validateDataHas(actionData, PROPERTY_TO_ADDRESS);
        validateDataHas(actionData, PROPERTY_SUBJECT);
        validateDataHas(actionData, PROPERTY_BODY);
    }

    @Override
    public void execute(ActionExecuteParameters parameters, User user) {
        String fromAddress = parameters.getData().getString(PROPERTY_FROM_ADDRESS);
        String toAddress = parameters.getData().getString(PROPERTY_TO_ADDRESS);
        String subject = parameters.getData().getString(PROPERTY_SUBJECT);
        String body = parameters.getData().getString(PROPERTY_BODY);
        LOGGER.debug("sending email from %s to %s subject %s\n%s", fromAddress, toAddress, subject, body);
        emailRepository.send(fromAddress, toAddress, subject, body);
    }
}
