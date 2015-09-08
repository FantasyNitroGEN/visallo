package org.visallo.core.email;

import com.google.common.base.Joiner;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public class NopEmailRepository implements EmailRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(NopEmailRepository.class);

    @Override
    public void send(String fromAddress, String toAddress, String subject, String body) {
        send(fromAddress, new String[]{toAddress}, subject, body);
    }

    @Override
    public void send(String fromAddress, String[] toAddresses, String subject, String body) {
        LOGGER.info("Send - fromAddress: %s, toAddresses: %s, subject: %s\n%s", fromAddress, Joiner.on(",").join(toAddresses), subject, body);
    }
}
