package org.visallo.core.model.user;

import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.*;

public final class UserVisalloProperties {
    public static final StringSingleValueVisalloProperty USERNAME = new StringSingleValueVisalloProperty("http://visallo.org/user#username");
    public static final StringSingleValueVisalloProperty DISPLAY_NAME = new StringSingleValueVisalloProperty("http://visallo.org/user#displayName");
    public static final StringSingleValueVisalloProperty EMAIL_ADDRESS = new StringSingleValueVisalloProperty("http://visallo.org/user#emailAddress");
    public static final DateSingleValueVisalloProperty CREATE_DATE = new DateSingleValueVisalloProperty("http://visallo.org/user#createDate");
    public static final DateSingleValueVisalloProperty CURRENT_LOGIN_DATE = new DateSingleValueVisalloProperty("http://visallo.org/user#currentLoginDate");
    public static final StringSingleValueVisalloProperty CURRENT_LOGIN_REMOTE_ADDR = new StringSingleValueVisalloProperty("http://visallo.org/user#currentLoginRemoteAddr");
    public static final DateSingleValueVisalloProperty PREVIOUS_LOGIN_DATE = new DateSingleValueVisalloProperty("http://visallo.org/user#previousLoginDate");
    public static final StringSingleValueVisalloProperty PREVIOUS_LOGIN_REMOTE_ADDR = new StringSingleValueVisalloProperty("http://visallo.org/user#previousLoginRemoteAddr");
    public static final IntegerSingleValueVisalloProperty LOGIN_COUNT = new IntegerSingleValueVisalloProperty("http://visallo.org/user#loginCount");
    public static final StringSingleValueVisalloProperty STATUS = new StringSingleValueVisalloProperty("http://visallo.org/user#status");
    public static final StringSingleValueVisalloProperty CURRENT_WORKSPACE = new StringSingleValueVisalloProperty("http://visallo.org/user#currentWorkspace");
    public static final JsonSingleValueVisalloProperty UI_PREFERENCES = new JsonSingleValueVisalloProperty("http://visallo.org/user#uiPreferences");
    public static final ByteArraySingleValueVisalloProperty PASSWORD_SALT = new ByteArraySingleValueVisalloProperty("http://visallo.org/user#passwordSalt");
    public static final ByteArraySingleValueVisalloProperty PASSWORD_HASH = new ByteArraySingleValueVisalloProperty("http://visallo.org/user#passwordHash");
    public static final StringSingleValueVisalloProperty PASSWORD_RESET_TOKEN = new StringSingleValueVisalloProperty("http://visallo.org/user#passwordResetToken");
    public static final DateSingleValueVisalloProperty PASSWORD_RESET_TOKEN_EXPIRATION_DATE = new DateSingleValueVisalloProperty("http://visallo.org/user#passwordResetTokenExpirationDate");

    public static boolean isBuiltInProperty(String propertyName) {
        return VisalloProperties.isBuiltInProperty(propertyName)
                || VisalloProperties.isBuiltInProperty(UserVisalloProperties.class, propertyName);
    }
}
