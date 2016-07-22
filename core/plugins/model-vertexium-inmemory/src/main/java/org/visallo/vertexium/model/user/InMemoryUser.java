package org.visallo.vertexium.model.user;

import com.google.common.collect.ImmutableMap;
import org.json.JSONObject;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.UserStatus;
import org.visallo.web.clientapi.model.UserType;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InMemoryUser implements User {
    private final String userId;
    private final String userName;
    private final String displayName;
    private final String emailAddress;
    private final Date createDate;
    private final String currentWorkspaceId;
    private JSONObject preferences;
    private Date currentLoginDate;
    private String currentLoginRemoteAddr;
    private Date previousLoginDate;
    private String previousLoginRemoteAddr;
    private int loginCount;
    private String passwordResetToken;
    private Date passwordResetTokenExpirationDate;
    private Map<String, Object> properties = new HashMap<>();

    public InMemoryUser(
            String userName,
            String displayName,
            String emailAddress,
            String currentWorkspaceId
    ) {
        this.userId = UUID.randomUUID().toString();
        this.userName = userName;
        this.displayName = displayName;
        this.emailAddress = emailAddress;
        this.createDate = new Date();
        this.currentWorkspaceId = currentWorkspaceId;
        this.preferences = new JSONObject();
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return userName;
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
        throw new RuntimeException("not implemented");
    }

    @Override
    public String getCurrentWorkspaceId() {
        return this.currentWorkspaceId;
    }

    @Override
    public JSONObject getUiPreferences() {
        return preferences;
    }

    public void setPreferences(JSONObject preferences) {
        this.preferences = preferences;
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
    public Object getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public Map<String, Object> getCustomProperties() {
        return ImmutableMap.copyOf(properties);
    }

    public void setProperty(String propertyName, Object value) {
        properties.put(propertyName, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InMemoryUser that = (InMemoryUser) o;

        if (!userId.equals(that.userId)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }

    @Override
    public String toString() {
        return "InMemoryUser{" +
                "userId='" + userId + '\'' +
                '}';
    }
}
