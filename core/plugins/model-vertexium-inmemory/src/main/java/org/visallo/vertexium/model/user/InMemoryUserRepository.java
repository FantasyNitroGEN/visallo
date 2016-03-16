package org.visallo.vertexium.model.user;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.notification.UserNotificationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.user.UserSessionCounterRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.ProxyUser;
import org.visallo.core.user.SystemUser;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.UserStatus;

import javax.inject.Inject;
import java.util.*;

public class InMemoryUserRepository extends UserRepository {
    private final Graph graph;
    private List<User> users = new ArrayList<>();

    @Inject
    public InMemoryUserRepository(
            Graph graph,
            Configuration configuration,
            SimpleOrmSession simpleOrmSession,
            UserSessionCounterRepository userSessionCounterRepository,
            WorkQueueRepository workQueueRepository,
            UserNotificationRepository userNotificationRepository,
            LockRepository lockRepository
    ) {
        super(
                configuration,
                simpleOrmSession,
                userSessionCounterRepository,
                workQueueRepository,
                userNotificationRepository,
                lockRepository
        );
        this.graph = graph;
    }

    @Override
    public User findByUsername(final String username) {
        return Iterables.find(this.users, new Predicate<User>() {
            @Override
            public boolean apply(User user) {
                return user.getUsername().equals(username);
            }
        }, null);
    }

    @Override
    public Iterable<User> find(int skip, int limit) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public User findById(final String userId) {
        return Iterables.find(this.users, new Predicate<User>() {
            @Override
            public boolean apply(User user) {
                return user.getUserId().equals(userId);
            }
        }, null);
    }

    @Override
    protected User addUser(String username, String displayName, String emailAddress, String password, Set<String> privileges, Set<String> userAuthorizations) {
        username = formatUsername(username);
        displayName = displayName.trim();
        InMemoryUser user = new InMemoryUser(username, displayName, emailAddress, privileges, userAuthorizations.toArray(new String[userAuthorizations.size()]), null);
        afterNewUserAdded(user);
        users.add(user);
        return user;
    }

    @Override
    public void setPassword(User user, String password) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isPasswordValid(User user, String password) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void recordLogin(User user, String remoteAddr) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public User setCurrentWorkspace(String userId, String workspaceId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String getCurrentWorkspaceId(String userId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setUiPreferences(User user, JSONObject preferences) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public User setStatus(String userId, UserStatus status) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void internalAddAuthorization(User user, String auth, User authUser) {
        ((InMemoryUser) user).addAuthorization(auth);
    }

    @Override
    public void internalRemoveAuthorization(User user, String auth, User authUser) {
        ((InMemoryUser) user).removeAuthorization(auth);
    }

    @Override
    public Authorizations getAuthorizations(User user, String... additionalAuthorizations) {
        List<String> auths = new ArrayList<>();
        if (user instanceof SystemUser) {
            auths.add(VisalloVisibility.SUPER_USER_VISIBILITY_STRING);
        } else if (user instanceof ProxyUser) {
            ProxyUser proxyUser = (ProxyUser) user;
            Collections.addAll(auths, ((InMemoryUser) proxyUser.getProxiedUser()).getAuthorizations());
        } else {
            Collections.addAll(auths, ((InMemoryUser) user).getAuthorizations());
        }
        Collections.addAll(auths, additionalAuthorizations);
        return this.graph.createAuthorizations(auths.toArray(new String[auths.size()]));
    }

    @Override
    public void setDisplayName(User user, String displayName) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setEmailAddress(User user, String emailAddress) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    protected void internalDelete(User user) {

    }

    @Override
    protected void internalSetPrivileges(User user, Set<String> privileges, User authUser) {
        ((InMemoryUser) user).setPrivileges(privileges);
    }

    @Override
    public User findByPasswordResetToken(String token) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setPasswordResetTokenAndExpirationDate(User user, String token, Date expirationDate) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void clearPasswordResetTokenAndExpirationDate(User user) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setPropertyOnUser(User user, String propertyName, Object value) {
        if (user instanceof SystemUser) {
            throw new VisalloException("Cannot set properties on system user");
        }
        ((InMemoryUser) user).setProperty(propertyName, value);
    }
}
