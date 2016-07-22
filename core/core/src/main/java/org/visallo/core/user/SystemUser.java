package org.visallo.core.user;

import org.json.JSONObject;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.clientapi.model.UserStatus;
import org.visallo.web.clientapi.model.UserType;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SystemUser implements User {
    private static final long serialVersionUID = 1L;
    public static final String USERNAME = "system";
    public static final String USER_ID = UserRepository.GRAPH_USER_ID_PREFIX + "system";

    public SystemUser() {
    }

    @Override
    public String getUserId() {
        return USER_ID;
    }

    @Override
    public String getUsername() {
        return USERNAME;
    }

    @Override
    public String getDisplayName() {
        return USERNAME;
    }

    @Override
    public String getEmailAddress() {
        return USERNAME;
    }

    @Override
    public Date getCreateDate() {
        return new Date(0);
    }

    @Override
    public Date getCurrentLoginDate() {
        return null;
    }

    @Override
    public String getCurrentLoginRemoteAddr() {
        return null;
    }

    @Override
    public Date getPreviousLoginDate() {
        return null;
    }

    @Override
    public String getPreviousLoginRemoteAddr() {
        return null;
    }

    @Override
    public int getLoginCount() {
        return 0;
    }

    @Override
    public UserType getUserType() {
        return UserType.SYSTEM;
    }

    @Override
    public UserStatus getUserStatus() {
        return UserStatus.OFFLINE;
    }

    @Override
    public String getCurrentWorkspaceId() {
        return null;
    }

    @Override
    public JSONObject getUiPreferences() {
        return new JSONObject();
    }

    @Override
    public String toString() {
        return "SystemUser";
    }

    @Override
    public String getPasswordResetToken() {
        return null;
    }

    @Override
    public Date getPasswordResetTokenExpirationDate() {
        return null;
    }

    @Override
    public Object getProperty(String propertyName) {
        return null;
    }

    @Override
    public Map<String, Object> getCustomProperties() {
        return new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SystemUser that = (SystemUser) o;

        if (!getUserId().equals(that.getUserId())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return getUserId().hashCode();
    }
}
