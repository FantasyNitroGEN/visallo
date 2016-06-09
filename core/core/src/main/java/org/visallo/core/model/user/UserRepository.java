package org.visallo.core.model.user;

import com.v5analytics.simpleorm.SimpleOrmContext;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.SystemUser;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.JSONUtil;
import org.visallo.web.clientapi.model.ClientApiUser;
import org.visallo.web.clientapi.model.ClientApiUsers;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.UserStatus;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

import static org.vertexium.util.IterableUtils.toList;

public abstract class UserRepository {
    public static final String GRAPH_USER_ID_PREFIX = "USER_";
    public static final String VISIBILITY_STRING = "user";
    public static final VisalloVisibility VISIBILITY = new VisalloVisibility(VISIBILITY_STRING);
    public static final String OWL_IRI = "http://visallo.org/user";
    public static final String USER_CONCEPT_IRI = "http://visallo.org/user#user";
    private final SimpleOrmSession simpleOrmSession;
    private final UserSessionCounterRepository userSessionCounterRepository;
    private final WorkQueueRepository workQueueRepository;
    private final LockRepository lockRepository;
    private final Configuration configuration;
    private final AuthorizationRepository authorizationRepository;
    private final PrivilegeRepository privilegeRepository;
    private LongRunningProcessRepository longRunningProcessRepository; // can't inject this because of circular dependencies
    private Collection<UserListener> userListeners;

    protected UserRepository(
            Configuration configuration,
            SimpleOrmSession simpleOrmSession,
            UserSessionCounterRepository userSessionCounterRepository,
            WorkQueueRepository workQueueRepository,
            LockRepository lockRepository,
            AuthorizationRepository authorizationRepository,
            PrivilegeRepository privilegeRepository
    ) {
        this.configuration = configuration;
        this.simpleOrmSession = simpleOrmSession;
        this.userSessionCounterRepository = userSessionCounterRepository;
        this.workQueueRepository = workQueueRepository;
        this.lockRepository = lockRepository;
        this.authorizationRepository = authorizationRepository;
        this.privilegeRepository = privilegeRepository;
    }

    public abstract User findByUsername(String username);

    public abstract Iterable<User> find(int skip, int limit);

    /*
    simple and likely slow implementation expected to be overridden in production implementations
     */
    public Iterable<User> findByStatus(int skip, int limit, UserStatus status) {
        List<User> allUsers = toList(find(skip, limit));
        List<User> matchingUsers = new ArrayList<>();
        for (User user : allUsers) {
            if (user.getUserStatus() == status) {
                matchingUsers.add(user);
            }
        }
        return matchingUsers;
    }

    public abstract User findById(String userId);

    protected abstract User addUser(
            String username,
            String displayName,
            String emailAddress,
            String password
    );

    public void setPassword(User user, String password) {
        byte[] salt = UserPasswordUtil.getSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);
        setPassword(user, salt, passwordHash);
    }

    public abstract void setPassword(User user, byte[] salt, byte[] passwordHash);

    public abstract boolean isPasswordValid(User user, String password);

    /**
     * Called by web authentication handlers when a user is authenticated
     */
    public abstract void updateUser(User user, AuthorizationContext authorizationContext);

    public abstract User setCurrentWorkspace(String userId, String workspaceId);

    public abstract String getCurrentWorkspaceId(String userId);

    public abstract User setStatus(String userId, UserStatus status);

    public abstract void setDisplayName(User user, String displayName);

    public abstract void setEmailAddress(User user, String emailAddress);

    public abstract void setUiPreferences(User user, JSONObject preferences);

    public JSONObject toJsonWithAuths(User user) {
        JSONObject json = toJson(user);

        JSONArray authorizations = new JSONArray();
        for (String a : authorizationRepository.getAuthorizations(user)) {
            authorizations.put(a);
        }
        json.put("authorizations", authorizations);

        json.put("uiPreferences", user.getUiPreferences());

        Set<String> privileges = privilegeRepository.getPrivileges(user);
        json.put("privileges", Privilege.toJson(privileges));

        return json;
    }

    /**
     * This is different from the non-private method in that it returns authorizations,
     * long running processes, etc for that user.
     */
    public ClientApiUser toClientApiPrivate(User user) {
        ClientApiUser u = toClientApi(user);

        for (String a : authorizationRepository.getAuthorizations(user)) {
            u.addAuthorization(a);
        }

        for (JSONObject json : getLongRunningProcesses(user)) {
            u.getLongRunningProcesses().add(ClientApiConverter.toClientApiValue(json));
        }

        u.setUiPreferences(JSONUtil.toJsonNode(user.getUiPreferences()));

        u.getProperties().putAll(user.getCustomProperties());

        Set<String> privileges = privilegeRepository.getPrivileges(user);
        u.getPrivileges().addAll(privileges);

        return u;
    }

    private List<JSONObject> getLongRunningProcesses(User user) {
        return getLongRunningProcessRepository().getLongRunningProcesses(user);
    }

    private LongRunningProcessRepository getLongRunningProcessRepository() {
        if (this.longRunningProcessRepository == null) {
            this.longRunningProcessRepository = InjectHelper.getInstance(LongRunningProcessRepository.class);
        }
        return this.longRunningProcessRepository;
    }

    public ClientApiUser toClientApi(User user) {
        return toClientApi(user, null);
    }

    private ClientApiUser toClientApi(User user, Map<String, String> workspaceNames) {
        ClientApiUser u = new ClientApiUser();
        u.setId(user.getUserId());
        u.setUserName(user.getUsername());
        u.setDisplayName(user.getDisplayName());
        u.setStatus(user.getUserStatus());
        u.setUserType(user.getUserType());
        u.setEmail(user.getEmailAddress());
        u.setCurrentWorkspaceId(user.getCurrentWorkspaceId());
        u.getProperties().putAll(user.getCustomProperties());
        if (workspaceNames != null) {
            String workspaceName = workspaceNames.get(user.getCurrentWorkspaceId());
            u.setCurrentWorkspaceName(workspaceName);
        }
        return u;
    }

    protected String formatUsername(String username) {
        return username.trim().toLowerCase();
    }

    public ClientApiUsers toClientApi(Iterable<User> users, Map<String, String> workspaceNames) {
        ClientApiUsers clientApiUsers = new ClientApiUsers();
        for (User user : users) {
            clientApiUsers.getUsers().add(toClientApi(user, workspaceNames));
        }
        return clientApiUsers;
    }

    public static JSONObject toJson(User user) {
        return toJson(user, null);
    }

    public static JSONObject toJson(User user, Map<String, String> workspaceNames) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", user.getUserId());
            json.put("userName", user.getUsername());
            json.put("displayName", user.getDisplayName());
            json.put("status", user.getUserStatus());
            json.put("userType", user.getUserType());
            json.put("email", user.getEmailAddress());
            json.put("currentWorkspaceId", user.getCurrentWorkspaceId());
            if (workspaceNames != null) {
                String workspaceName = workspaceNames.get(user.getCurrentWorkspaceId());
                json.put("currentWorkspaceName", workspaceName);
            }
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public SimpleOrmContext getSimpleOrmContext(User user) {
        Set<String> authorizationsSet = authorizationRepository.getAuthorizations(user);
        String[] authorizations = authorizationsSet.toArray(new String[authorizationsSet.size()]);
        return getSimpleOrmContext(authorizations);
    }

    public SimpleOrmContext getSimpleOrmContext(String... authorizations) {
        return simpleOrmSession.createContext(authorizations);
    }

    public User getSystemUser() {
        return new SystemUser();
    }

    public User findOrAddUser(
            String username,
            String displayName,
            String emailAddress,
            String password
    ) {
        return lockRepository.lock("findOrAddUser", () -> {
            User user = findByUsername(username);
            if (user == null) {
                user = addUser(username, displayName, emailAddress, password);
            }
            return user;
        });
    }

    public final void delete(User user) {
        internalDelete(user);
        userSessionCounterRepository.deleteSessions(user.getUserId());
        workQueueRepository.pushUserStatusChange(user, UserStatus.OFFLINE);
        fireUserDeletedEvent(user);
    }

    protected abstract void internalDelete(User user);

    public Iterable<User> find(String query) {
        final String lowerCaseQuery = query == null ? null : query.toLowerCase();

        int skip = 0;
        int limit = 100;
        List<User> foundUsers = new ArrayList<>();
        while (true) {
            List<User> users = toList(find(skip, limit));
            if (users.size() == 0) {
                break;
            }
            for (User user : users) {
                if (lowerCaseQuery == null || user.getDisplayName().toLowerCase().contains(lowerCaseQuery)) {
                    foundUsers.add(user);
                }
            }
            skip += limit;
        }
        return foundUsers;
    }

    public static String createRandomPassword() {
        return new BigInteger(120, new SecureRandom()).toString(32);
    }

    public abstract User findByPasswordResetToken(String token);

    public abstract void setPasswordResetTokenAndExpirationDate(User user, String token, Date expirationDate);

    public abstract void clearPasswordResetTokenAndExpirationDate(User user);

    protected void afterNewUserAdded(User newUser) {
        fireNewUserAddedEvent(newUser);
    }

    private void fireNewUserAddedEvent(User user) {
        for (UserListener userListener : getUserListeners()) {
            userListener.newUserAdded(user);
        }
    }

    private void fireUserDeletedEvent(User user) {
        for (UserListener userListener : getUserListeners()) {
            userListener.userDeleted(user);
        }
    }

    protected void fireUserLoginEvent(User user, AuthorizationContext authorizationContext) {
        for (UserListener userListener : getUserListeners()) {
            userListener.userLogin(user, authorizationContext);
        }
    }

    protected void fireUserStatusChangeEvent(User user, UserStatus status) {
        for (UserListener userListener : getUserListeners()) {
            userListener.userStatusChange(user, status);
        }
    }

    private Collection<UserListener> getUserListeners() {
        if (userListeners == null) {
            userListeners = InjectHelper.getInjectedServices(UserListener.class, configuration);
        }
        return userListeners;
    }

    public abstract void setPropertyOnUser(User user, String propertyName, Object value);

    protected AuthorizationRepository getAuthorizationRepository() {
        return authorizationRepository;
    }

    protected PrivilegeRepository getPrivilegeRepository() {
        return privilegeRepository;
    }
}