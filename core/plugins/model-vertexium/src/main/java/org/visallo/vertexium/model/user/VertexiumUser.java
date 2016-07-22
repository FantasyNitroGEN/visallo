package org.visallo.vertexium.model.user;

import com.google.common.collect.ImmutableMap;
import org.json.JSONObject;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.visallo.core.model.user.UserVisalloProperties;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.UserStatus;
import org.visallo.web.clientapi.model.UserType;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class VertexiumUser implements User, Serializable {
    private static final long serialVersionUID = 6688073934273514248L;
    private final String userId;
    private final Map<String, Object> properties = new HashMap<>();

    public VertexiumUser(Vertex userVertex) {
        this.userId = userVertex.getId();
        for (Property property : userVertex.getProperties()) {
            this.properties.put(property.getName(), property.getValue());
        }
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return UserVisalloProperties.USERNAME.getPropertyValue(properties);
    }

    @Override
    public String getDisplayName() {
        return UserVisalloProperties.DISPLAY_NAME.getPropertyValue(properties);
    }

    @Override
    public String getEmailAddress() {
        return UserVisalloProperties.EMAIL_ADDRESS.getPropertyValue(properties);
    }

    @Override
    public Date getCreateDate() {
        return UserVisalloProperties.CREATE_DATE.getPropertyValue(properties);
    }

    @Override
    public Date getCurrentLoginDate() {
        return UserVisalloProperties.CURRENT_LOGIN_DATE.getPropertyValue(properties);
    }

    @Override
    public String getCurrentLoginRemoteAddr() {
        return UserVisalloProperties.CURRENT_LOGIN_REMOTE_ADDR.getPropertyValue(properties);
    }

    @Override
    public Date getPreviousLoginDate() {
        return UserVisalloProperties.PREVIOUS_LOGIN_DATE.getPropertyValue(properties);
    }

    @Override
    public String getPreviousLoginRemoteAddr() {
        return UserVisalloProperties.PREVIOUS_LOGIN_REMOTE_ADDR.getPropertyValue(properties);
    }

    @Override
    public int getLoginCount() {
        return UserVisalloProperties.LOGIN_COUNT.getPropertyValue(properties, 0);
    }

    @Override
    public UserType getUserType() {
        return UserType.USER;
    }

    @Override
    public UserStatus getUserStatus() {
        return UserStatus.valueOf(UserVisalloProperties.STATUS.getPropertyValue(properties));
    }

    public void setUserStatus(UserStatus status) {
        UserVisalloProperties.STATUS.setProperty(properties, status.name());
    }

    @Override
    public String getCurrentWorkspaceId() {
        return UserVisalloProperties.CURRENT_WORKSPACE.getPropertyValue(properties);
    }

    @Override
    public JSONObject getUiPreferences() {
        JSONObject preferences = UserVisalloProperties.UI_PREFERENCES.getPropertyValue(properties);
        if (preferences == null) {
            preferences = new JSONObject();
            UserVisalloProperties.UI_PREFERENCES.setProperty(properties, preferences);
        }
        return preferences;
    }

    @Override
    public String getPasswordResetToken() {
        return UserVisalloProperties.PASSWORD_RESET_TOKEN.getPropertyValue(properties);
    }

    @Override
    public Date getPasswordResetTokenExpirationDate() {
        return UserVisalloProperties.PASSWORD_RESET_TOKEN_EXPIRATION_DATE.getPropertyValue(properties);
    }

    @Override
    public Object getProperty(String propertyName) {
        return this.properties.get(propertyName);
    }

    @Override
    public Map<String, Object> getCustomProperties() {
        Map<String, Object> results = new HashMap<>();
        for (Map.Entry<String, Object> property : properties.entrySet()) {
            if (!UserVisalloProperties.isBuiltInProperty(property.getKey())) {
                results.put(property.getKey(), property.getValue());
            }
        }
        return ImmutableMap.copyOf(results);
    }

    public void setProperty(String propertyName, Object value) {
        this.properties.put(propertyName, value);
    }

    @Override
    public String toString() {
        return "VertexiumUser{userId='" + getUserId() + "', displayName='" + getDisplayName() + "}";
    }
}
