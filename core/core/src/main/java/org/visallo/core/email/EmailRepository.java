package org.visallo.core.email;

public interface EmailRepository {
    void send(String fromAddress, String toAddress, String subject, String body);
    void send(String fromAddress, String toAddresses[], String subject, String body);
}
