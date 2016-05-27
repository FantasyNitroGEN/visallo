package org.visallo.core.email;

import java.util.ArrayList;
import java.util.List;

public class MockEmailRepository implements EmailRepository {
    public final List<Message> messages = new ArrayList<>();

    @Override
    public void send(String fromAddress, String toAddress, String subject, String body) {
        send(fromAddress, new String[]{toAddress}, subject, body);
    }

    @Override
    public void send(String fromAddress, String[] toAddresses, String subject, String body) {
        messages.add(new Message(fromAddress, toAddresses, subject, body));
    }

    public static class Message {
        public final String fromAddress;
        public final String[] toAddresses;
        public final String subject;
        public final String body;

        public Message(String fromAddress, String[] toAddresses, String subject, String body) {
            this.fromAddress = fromAddress;
            this.toAddresses = toAddresses;
            this.subject = subject;
            this.body = body;
        }
    }
}
