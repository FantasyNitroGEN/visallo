package org.visallo.core.user;

import org.json.JSONObject;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.clientapi.model.UserStatus;
import org.visallo.web.clientapi.model.UserType;

import java.util.Date;
import java.util.Map;

/**
 * This class is used to store the userId only in a web session. If we were to store the entire
 * user object in the session, any changes to the user would not be reflected unless the user object
 * was refreshed.
 */
public class ProxyUser implements User {
    private static final long serialVersionUID = -7652656758524792116L;
    private final String userId;
    private final UserRepository userRepository;
    private User proxiedUser;

    public ProxyUser(String userId, UserRepository userRepository) {
        this.userId = userId;
        this.userRepository = userRepository;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    public User getProxiedUser() {
        ensureUser();
        return proxiedUser;
    }

    @Override
    public String getUsername() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getUsername();
    }

    @Override
    public String getDisplayName() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getDisplayName();
    }

    @Override
    public String getEmailAddress() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getEmailAddress();
    }

    @Override
    public Date getCreateDate() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getCreateDate();
    }

    @Override
    public Date getCurrentLoginDate() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getCurrentLoginDate();
    }

    @Override
    public String getCurrentLoginRemoteAddr() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getCurrentLoginRemoteAddr();
    }

    @Override
    public Date getPreviousLoginDate() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getPreviousLoginDate();
    }

    @Override
    public String getPreviousLoginRemoteAddr() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getPreviousLoginRemoteAddr();
    }

    @Override
    public int getLoginCount() {
        ensureUser();
        if (proxiedUser == null) {
            return 0;
        }
        return proxiedUser.getLoginCount();
    }

    @Override
    public UserType getUserType() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getUserType();
    }

    @Override
    public UserStatus getUserStatus() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getUserStatus();
    }

    @Override
    public String getCurrentWorkspaceId() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getCurrentWorkspaceId();
    }

    @Override
    public JSONObject getUiPreferences() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getUiPreferences();
    }

    @Override
    public String getPasswordResetToken() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getPasswordResetToken();
    }

    @Override
    public Date getPasswordResetTokenExpirationDate() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getPasswordResetTokenExpirationDate();
    }

    @Override
    public Object getProperty(String propertyName) {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getProperty(propertyName);
    }

    @Override
    public Map<String, Object> getCustomProperties() {
        ensureUser();
        if (proxiedUser == null) {
            return null;
        }
        return proxiedUser.getCustomProperties();
    }

    private void ensureUser() {
        if (proxiedUser == null) {
            proxiedUser = userRepository.findById(userId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof User)) {
            return false;
        }

        User other = (User) o;
        return getUserId().equals(other.getUserId());
    }

    @Override
    public int hashCode() {
        return getUserId().hashCode();
    }

    @Override
    public String toString() {
        return "ProxyUser{" +
                "userId='" + userId + '\'' +
                '}';
    }
}
