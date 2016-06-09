package org.visallo.core.user;

import org.json.JSONObject;
import org.visallo.web.clientapi.model.UserStatus;
import org.visallo.web.clientapi.model.UserType;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public interface User extends Serializable {
    long serialVersionUID = 2L;

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

    String getPasswordResetToken();

    Date getPasswordResetTokenExpirationDate();

    Object getProperty(String propertyName);

    Map<String, Object> getCustomProperties();
}
