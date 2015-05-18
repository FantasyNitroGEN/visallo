package org.visallo.vertexium.model.user;

import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.UserStatus;
import org.visallo.web.clientapi.model.UserType;
import org.json.JSONObject;
import com.v5analytics.simpleorm.SimpleOrmContext;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

public class VertexiumUser implements User, Serializable {
    private static final long serialVersionUID = 6688073934273514248L;
    private SimpleOrmContext simpleOrmContext;
    private String userId;
    private String username;
    private String displayName;
    private String emailAddress;
    private Date createDate;
    private Date currentLoginDate;
    private String currentLoginRemoteAddr;
    private Date previousLoginDate;
    private String previousLoginRemoteAddr;
    private int loginCount;
    private UserStatus userStatus;
    private Set<Privilege> privileges;
    private String currentWorkspaceId;
    private JSONObject preferences;
    private String passwordResetToken;
    private Date passwordResetTokenExpirationDate;

    // required for Serializable
    protected VertexiumUser() {

    }

    public VertexiumUser(
            String userId,
            String username,
            String displayName,
            String emailAddress,
            Date createDate,
            Date currentLoginDate,
            String currentLoginRemoteAddr,
            Date previousLoginDate,
            String previousLoginRemoteAddr,
            int loginCount,
            SimpleOrmContext simpleOrmContext,
            UserStatus userStatus,
            Set<Privilege> privileges,
            String currentWorkspaceId,
            JSONObject preferences,
            String passwordResetToken,
            Date passwordResetTokenExpirationDate
    ) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.emailAddress = emailAddress;
        this.createDate = createDate;
        this.currentLoginDate = currentLoginDate;
        this.currentLoginRemoteAddr = currentLoginRemoteAddr;
        this.previousLoginDate = previousLoginDate;
        this.previousLoginRemoteAddr = previousLoginRemoteAddr;
        this.loginCount = loginCount;
        this.simpleOrmContext = simpleOrmContext;
        this.userStatus = userStatus;
        this.privileges = privileges;
        this.currentWorkspaceId = currentWorkspaceId;
        this.preferences = preferences;
        if (this.preferences == null) {
            this.preferences = new JSONObject();
        }
        this.passwordResetToken = passwordResetToken;
        this.passwordResetTokenExpirationDate = passwordResetTokenExpirationDate;
    }

    @Override
    public SimpleOrmContext getSimpleOrmContext() {
        return simpleOrmContext;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getEmailAddress() {
        return emailAddress;
    }

    @Override
    public Date getCreateDate() {
        return createDate;
    }

    @Override
    public Date getCurrentLoginDate() {
        return currentLoginDate;
    }

    @Override
    public String getCurrentLoginRemoteAddr() {
        return currentLoginRemoteAddr;
    }

    @Override
    public Date getPreviousLoginDate() {
        return previousLoginDate;
    }

    @Override
    public String getPreviousLoginRemoteAddr() {
        return previousLoginRemoteAddr;
    }

    @Override
    public int getLoginCount() {
        return loginCount;
    }

    @Override
    public UserType getUserType() {
        return UserType.USER;
    }

    @Override
    public UserStatus getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(UserStatus status) {
        this.userStatus = status;
    }

    @Override
    public Set<Privilege> getPrivileges() {
        return privileges;
    }

    @Override
    public String getCurrentWorkspaceId() {
        return currentWorkspaceId;
    }

    @Override
    public JSONObject getUiPreferences() {
        return preferences;
    }

    @Override
    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    @Override
    public Date getPasswordResetTokenExpirationDate() {
        return passwordResetTokenExpirationDate;
    }

    @Override
    public String toString() {
        return "VertexiumUser{userId='" + getUserId() + "', displayName='" + getDisplayName() + "', privileges=" + getPrivileges() + "}";
    }
}
