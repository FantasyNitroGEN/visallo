package org.visallo.core.model.notification;

import org.visallo.core.exception.VisalloException;
import org.apache.commons.codec.binary.Hex;
import com.v5analytics.simpleorm.SimpleOrmSession;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class NotificationRepository {
    private final SimpleOrmSession simpleOrmSession;

    protected NotificationRepository(SimpleOrmSession simpleOrmSession) {
        this.simpleOrmSession = simpleOrmSession;
    }

    protected static String hash(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] md5 = digest.digest(s.getBytes());
            return Hex.encodeHexString(md5);
        } catch (NoSuchAlgorithmException e) {
            throw new VisalloException("Could not find MD5", e);
        }
    }

    public SimpleOrmSession getSimpleOrmSession() {
        return simpleOrmSession;
    }
}
