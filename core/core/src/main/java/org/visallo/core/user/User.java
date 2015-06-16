package org.visallo.core.user;

import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.UserStatus;
import org.visallo.web.clientapi.model.UserType;
import org.json.JSONObject;
import com.v5analytics.simpleorm.SimpleOrmContext;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

public interface User extends Serializable {
    long serialVersionUID = 2L;

    SimpleOrmContext getSimpleOrmContext();

    String getUserId();

    String getUsername();

    String getDisplayName();

    String getEmailAddress();

    Date getCreateDate();

    Date getCurrentLoginDate();

    String getCurrentLoginRemoteAddr();

    Date getPreviousLoginDate();

    String getPreviousLoginRemoteAddr();

    int getLoginCount();

    UserType getUserType();

    UserStatus getUserStatus();

    String getCurrentWorkspaceId();

    JSONObject getUiPreferences();

    Set<Privilege> getPrivileges();

    String getPasswordResetToken();

    Date getPasswordResetTokenExpirationDate();
}
