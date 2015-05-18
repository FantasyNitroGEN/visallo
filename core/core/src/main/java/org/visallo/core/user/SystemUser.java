package org.visallo.core.user;

import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.UserStatus;
import org.visallo.web.clientapi.model.UserType;
import org.json.JSONObject;
import com.v5analytics.simpleorm.SimpleOrmContext;

import java.util.Date;
import java.util.Set;

public class SystemUser implements User {
    private static final long serialVersionUID = 1L;
    public static final String USERNAME = "system";
    private final SimpleOrmContext simpleOrmContext;

    public SystemUser(SimpleOrmContext simpleOrmContext) {
        this.simpleOrmContext = simpleOrmContext;
    }

    @Override
    public SimpleOrmContext getSimpleOrmContext() {
        return simpleOrmContext;
    }

    @Override
    public String getUserId() {
        return "";
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
    public Set<Privilege> getPrivileges() {
        return null;
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
}
