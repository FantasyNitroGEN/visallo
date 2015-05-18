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
    public static final long serialVersionUID = 2L;

    public SimpleOrmContext getSimpleOrmContext();

    public String getUserId();

    public String getUsername();

    public String getDisplayName();

    public String getEmailAddress();

    public Date getCreateDate();

    public Date getCurrentLoginDate();

    public String getCurrentLoginRemoteAddr();

    public Date getPreviousLoginDate();

    public String getPreviousLoginRemoteAddr();

    public int getLoginCount();

    public UserType getUserType();

    public UserStatus getUserStatus();

    public String getCurrentWorkspaceId();

    public JSONObject getUiPreferences();

    public Set<Privilege> getPrivileges();

    public String getPasswordResetToken();

    public Date getPasswordResetTokenExpirationDate();
}
