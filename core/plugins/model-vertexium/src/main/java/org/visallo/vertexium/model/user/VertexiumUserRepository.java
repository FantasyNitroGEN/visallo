package org.visallo.vertexium.model.user;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.v5analytics.simpleorm.SimpleOrmContext;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.VertexBuilder;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.util.ConvertingIterable;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.notification.UserNotificationRepository;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.*;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.trace.Traced;
import org.visallo.core.user.SystemUser;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.Privilege;
import org.visallo.web.clientapi.model.UserStatus;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.vertexium.util.IterableUtils.singleOrDefault;
import static org.vertexium.util.IterableUtils.toArray;

@Singleton
public class VertexiumUserRepository extends UserRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexiumUserRepository.class);
    private final AuthorizationRepository authorizationRepository;
    private Graph graph;
    private String userConceptId;
    private org.vertexium.Authorizations authorizations;
    private final Cache<String, Vertex> userVertexCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();

    @Inject
    public VertexiumUserRepository(
            Configuration configuration,
            SimpleOrmSession simpleOrmSession,
            AuthorizationRepository authorizationRepository,
            Graph graph,
            OntologyRepository ontologyRepository,
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
        this.authorizationRepository = authorizationRepository;
        this.graph = graph;

        authorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
        authorizationRepository.addAuthorizationToGraph(VisalloVisibility.SUPER_USER_VISIBILITY_STRING);

        Concept userConcept = ontologyRepository.getOrCreateConcept(null, USER_CONCEPT_IRI, "visalloUser", null);
        userConceptId = userConcept.getIRI();

        Set<String> authorizationsSet = new HashSet<>();
        authorizationsSet.add(VISIBILITY_STRING);
        authorizationsSet.add(VisalloVisibility.SUPER_USER_VISIBILITY_STRING);
        this.authorizations = graph.createAuthorizations(authorizationsSet);
    }

    private VertexiumUser createFromVertex(Vertex user) {
        if (user == null) {
            return null;
        }
        String[] authorizations = toArray(getAuthorizations(user), String.class);
        SimpleOrmContext simpleOrmContext = getSimpleOrmContext(authorizations);

        LOGGER.debug("Creating user from UserRow. username: %s", UserVisalloProperties.USERNAME.getPropertyValue(user));
        return new VertexiumUser(user, simpleOrmContext);
    }

    @Override
    public User findByUsername(String username) {
        username = formatUsername(username);
        Iterable<Vertex> vertices = graph.query(authorizations)
                .has(UserVisalloProperties.USERNAME.getPropertyName(), username)
                .has(VisalloProperties.CONCEPT_TYPE.getPropertyName(), userConceptId)
                .vertices();
        Vertex userVertex = singleOrDefault(vertices, null);
        if (userVertex == null) {
            return null;
        }
        userVertexCache.put(userVertex.getId(), userVertex);
        return createFromVertex(userVertex);
    }

    @Override
    public Iterable<User> find(int skip, int limit) {
        return new ConvertingIterable<Vertex, User>(graph.query(authorizations)
                .has(VisalloProperties.CONCEPT_TYPE.getPropertyName(), userConceptId)
                .skip(skip)
                .limit(limit)
                .vertices()) {
            @Override
            protected User convert(Vertex vertex) {
                return createFromVertex(vertex);
            }
        };
    }

    @Override
    public Iterable<User> findByStatus(int skip, int limit, UserStatus status) {
        return new ConvertingIterable<Vertex, User>(graph.query(authorizations)
                .has(VisalloProperties.CONCEPT_TYPE.getPropertyName(), userConceptId)
                .has(UserVisalloProperties.STATUS.getPropertyName(), status.toString())
                .skip(skip)
                .limit(limit)
                .vertices()) {
            @Override
            protected User convert(Vertex vertex) {
                return createFromVertex(vertex);
            }
        };
    }

    @Override
    @Traced
    public User findById(String userId) {
        if (SystemUser.USER_ID.equals(userId)) {
            return getSystemUser();
        }
        return createFromVertex(findByIdUserVertex(userId));
    }

    @Traced
    public Vertex findByIdUserVertex(String userId) {
        Vertex userVertex = userVertexCache.getIfPresent(userId);
        if (userVertex != null) {
            return userVertex;
        }
        userVertex = graph.getVertex(userId, authorizations);
        if (userVertex != null) {
            userVertexCache.put(userId, userVertex);
        }
        return userVertex;
    }

    @Override
    protected User addUser(String username, String displayName, String emailAddress, String password, String[] userAuthorizations) {
        username = formatUsername(username);
        displayName = displayName.trim();
        String authorizationsString = StringUtils.join(userAuthorizations, ",");

        byte[] salt = UserPasswordUtil.getSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);

        String id = GRAPH_USER_ID_PREFIX + graph.getIdGenerator().nextId();
        VertexBuilder userBuilder = graph.prepareVertex(id, VISIBILITY.getVisibility());

        VisalloProperties.CONCEPT_TYPE.setProperty(userBuilder, userConceptId, VISIBILITY.getVisibility());
        UserVisalloProperties.USERNAME.setProperty(userBuilder, username, VISIBILITY.getVisibility());
        UserVisalloProperties.DISPLAY_NAME.setProperty(userBuilder, displayName, VISIBILITY.getVisibility());
        UserVisalloProperties.CREATE_DATE.setProperty(userBuilder, new Date(), VISIBILITY.getVisibility());
        UserVisalloProperties.PASSWORD_SALT.setProperty(userBuilder, salt, VISIBILITY.getVisibility());
        UserVisalloProperties.PASSWORD_HASH.setProperty(userBuilder, passwordHash, VISIBILITY.getVisibility());
        UserVisalloProperties.STATUS.setProperty(userBuilder, UserStatus.OFFLINE.toString(), VISIBILITY.getVisibility());
        UserVisalloProperties.AUTHORIZATIONS.setProperty(userBuilder, authorizationsString, VISIBILITY.getVisibility());
        UserVisalloProperties.PRIVILEGES.setProperty(userBuilder, Privilege.toString(getDefaultPrivileges()), VISIBILITY.getVisibility());

        if (emailAddress != null) {
            UserVisalloProperties.EMAIL_ADDRESS.setProperty(userBuilder, emailAddress, VISIBILITY.getVisibility());
        }

        User user = createFromVertex(userBuilder.save(this.authorizations));
        graph.flush();

        afterNewUserAdded(user);

        return user;
    }

    @Override
    public void setPassword(User user, String password) {
        byte[] salt = UserPasswordUtil.getSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserVisalloProperties.PASSWORD_SALT.setProperty(userVertex, salt, VISIBILITY.getVisibility(), authorizations);
        UserVisalloProperties.PASSWORD_HASH.setProperty(userVertex, passwordHash, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
    }

    @Override
    public boolean isPasswordValid(User user, String password) {
        try {
            Vertex userVertex = findByIdUserVertex(user.getUserId());
            return UserPasswordUtil.validatePassword(password, UserVisalloProperties.PASSWORD_SALT.getPropertyValue(userVertex), UserVisalloProperties.PASSWORD_HASH.getPropertyValue(userVertex));
        } catch (Exception ex) {
            throw new RuntimeException("error validating password", ex);
        }
    }

    @Override
    public void recordLogin(User user, String remoteAddr) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        ExistingElementMutation<Vertex> m = userVertex.prepareMutation();

        Date currentLoginDate = UserVisalloProperties.CURRENT_LOGIN_DATE.getPropertyValue(userVertex);
        if (currentLoginDate != null) {
            UserVisalloProperties.PREVIOUS_LOGIN_DATE.setProperty(m, currentLoginDate, VISIBILITY.getVisibility());
        }

        String currentLoginRemoteAddr = UserVisalloProperties.CURRENT_LOGIN_REMOTE_ADDR.getPropertyValue(userVertex);
        if (currentLoginRemoteAddr != null) {
            UserVisalloProperties.PREVIOUS_LOGIN_REMOTE_ADDR.setProperty(m, currentLoginRemoteAddr, VISIBILITY.getVisibility());
        }

        UserVisalloProperties.CURRENT_LOGIN_DATE.setProperty(m, new Date(), VISIBILITY.getVisibility());
        UserVisalloProperties.CURRENT_LOGIN_REMOTE_ADDR.setProperty(m, remoteAddr, VISIBILITY.getVisibility());

        int loginCount = UserVisalloProperties.LOGIN_COUNT.getPropertyValue(userVertex, 0);
        UserVisalloProperties.LOGIN_COUNT.setProperty(m, loginCount + 1, VISIBILITY.getVisibility());

        m.save(authorizations);
        graph.flush();
    }

    @Override
    public User setCurrentWorkspace(String userId, String workspaceId) {
        User user = findById(userId);
        checkNotNull(user, "Could not find user: " + userId);
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserVisalloProperties.CURRENT_WORKSPACE.setProperty(userVertex, workspaceId, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
        return user;
    }

    @Override
    public String getCurrentWorkspaceId(String userId) {
        User user = findById(userId);
        checkNotNull(user, "Could not find user: " + userId);
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        return UserVisalloProperties.CURRENT_WORKSPACE.getPropertyValue(userVertex);
    }

    @Override
    public void setUiPreferences(User user, JSONObject preferences) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserVisalloProperties.UI_PREFERENCES.setProperty(userVertex, preferences, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
    }

    @Override
    public User setStatus(String userId, UserStatus status) {
        VertexiumUser user = (VertexiumUser) findById(userId);
        checkNotNull(user, "Could not find user: " + userId);
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserVisalloProperties.STATUS.setProperty(userVertex, status.toString(), VISIBILITY.getVisibility(), authorizations);
        graph.flush();
        user.setUserStatus(status);
        return user;
    }

    @Override
    public void internalAddAuthorization(User user, String auth, User authUser) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        Set<String> authorizationSet = getAuthorizations(userVertex);
        if (authorizationSet.contains(auth)) {
            return;
        }
        authorizationSet.add(auth);

        this.authorizationRepository.addAuthorizationToGraph(auth);

        String authorizationsString = StringUtils.join(authorizationSet, ",");
        UserVisalloProperties.AUTHORIZATIONS.setProperty(userVertex, authorizationsString, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
        userVertexCache.invalidate(user.getUserId());
    }

    @Override
    public void internalRemoveAuthorization(User user, String auth, User authUser) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        Set<String> authorizationSet = getAuthorizations(userVertex);
        if (!authorizationSet.contains(auth)) {
            return;
        }
        authorizationSet.remove(auth);
        String authorizationsString = StringUtils.join(authorizationSet, ",");
        UserVisalloProperties.AUTHORIZATIONS.setProperty(userVertex, authorizationsString, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
        userVertexCache.invalidate(user.getUserId());
    }

    @Override
    @Traced
    public org.vertexium.Authorizations getAuthorizations(User user, String... additionalAuthorizations) {
        Set<String> userAuthorizations;
        if (user instanceof SystemUser) {
            userAuthorizations = new HashSet<>();
            userAuthorizations.add(VisalloVisibility.SUPER_USER_VISIBILITY_STRING);
        } else {
            Vertex userVertex = userVertexCache.getIfPresent(user.getUserId());
            if (userVertex != null) {
                userAuthorizations = getAuthorizations(userVertex);
            } else {
                userAuthorizations = null;
            }
        }
        if (userAuthorizations == null) {
            LOGGER.debug("BEGIN getAuthorizations query");
            Vertex userVertex = findByIdUserVertex(user.getUserId());
            userAuthorizations = getAuthorizations(userVertex);
            LOGGER.debug("END getAuthorizations query");
        }

        Set<String> authorizationsSet = new HashSet<>(userAuthorizations);
        Collections.addAll(authorizationsSet, additionalAuthorizations);
        return graph.createAuthorizations(authorizationsSet);
    }

    public static Set<String> getAuthorizations(Vertex userVertex) {
        String authorizationsString = UserVisalloProperties.AUTHORIZATIONS.getPropertyValue(userVertex);
        if (authorizationsString == null) {
            return new HashSet<>();
        }
        String[] authorizationsArray = authorizationsString.split(",");
        if (authorizationsArray.length == 1 && authorizationsArray[0].length() == 0) {
            authorizationsArray = new String[0];
        }
        HashSet<String> authorizations = new HashSet<>();
        for (String s : authorizationsArray) {
            // Accumulo doesn't like zero length strings. they shouldn't be in the auth string to begin with but this just protects from that happening.
            if (s.trim().length() == 0) {
                continue;
            }

            authorizations.add(s);
        }
        return authorizations;
    }

    @Override
    public void setDisplayName(User user, String displayName) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserVisalloProperties.DISPLAY_NAME.setProperty(userVertex, displayName, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
    }

    @Override
    public void setEmailAddress(User user, String emailAddress) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserVisalloProperties.EMAIL_ADDRESS.setProperty(userVertex, emailAddress, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
    }

    @Override
    protected void internalDelete(User user) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        graph.softDeleteVertex(userVertex, authorizations);
        graph.flush();
    }

    @Override
    protected void internalSetPrivileges(User user, Set<String> privileges, User authUser) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserVisalloProperties.PRIVILEGES.setProperty(userVertex, Privilege.toString(privileges), VISIBILITY.getVisibility(), authorizations);
        graph.flush();
        userVertexCache.invalidate(user.getUserId());
    }

    @Override
    public User findByPasswordResetToken(String token) {
        return createFromVertex(singleOrDefault(graph.query(authorizations)
                .has(UserVisalloProperties.PASSWORD_RESET_TOKEN.getPropertyName(), token)
                .has(VisalloProperties.CONCEPT_TYPE.getPropertyName(), userConceptId)
                .vertices(), null));
    }

    @Override
    public void setPasswordResetTokenAndExpirationDate(User user, String token, Date expirationDate) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserVisalloProperties.PASSWORD_RESET_TOKEN.setProperty(userVertex, token, VISIBILITY.getVisibility(), authorizations);
        UserVisalloProperties.PASSWORD_RESET_TOKEN_EXPIRATION_DATE.setProperty(userVertex, expirationDate, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
    }

    @Override
    public void clearPasswordResetTokenAndExpirationDate(User user) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserVisalloProperties.PASSWORD_RESET_TOKEN.removeProperty(userVertex, authorizations);
        UserVisalloProperties.PASSWORD_RESET_TOKEN_EXPIRATION_DATE.removeProperty(userVertex, authorizations);
        graph.flush();
    }

    @Override
    public void setPropertyOnUser(User user, String propertyName, Object value) {
        if (user instanceof SystemUser) {
            throw new VisalloException("Cannot set properties on system user");
        }
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        userVertex.setProperty(propertyName, value, VISIBILITY.getVisibility(), authorizations);
        if (user instanceof VertexiumUser) {
            ((VertexiumUser) user).setProperty(propertyName, value);
        }
    }
}
