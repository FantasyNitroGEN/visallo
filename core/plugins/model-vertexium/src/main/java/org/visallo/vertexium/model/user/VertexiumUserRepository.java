package org.visallo.vertexium.model.user;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.v5analytics.simpleorm.SimpleOrmSession;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.VertexBuilder;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.query.QueryResultsIterable;
import org.vertexium.util.ConvertingIterable;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.lock.LockRepository;
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
import org.visallo.web.clientapi.model.UserStatus;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.vertexium.util.IterableUtils.singleOrDefault;

@Singleton
public class VertexiumUserRepository extends UserRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexiumUserRepository.class);
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
            GraphAuthorizationRepository graphAuthorizationRepository,
            Graph graph,
            OntologyRepository ontologyRepository,
            UserSessionCounterRepository userSessionCounterRepository,
            WorkQueueRepository workQueueRepository,
            LockRepository lockRepository,
            AuthorizationRepository authorizationRepository,
            PrivilegeRepository privilegeRepository
    ) {
        super(
                configuration,
                simpleOrmSession,
                userSessionCounterRepository,
                workQueueRepository,
                lockRepository,
                authorizationRepository,
                privilegeRepository
        );
        this.graph = graph;

        graphAuthorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
        graphAuthorizationRepository.addAuthorizationToGraph(VisalloVisibility.SUPER_USER_VISIBILITY_STRING);

        Concept userConcept = ontologyRepository.getOrCreateConcept(null, USER_CONCEPT_IRI, "visalloUser", null, false);
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

        LOGGER.debug("Creating user from UserRow. username: %s", UserVisalloProperties.USERNAME.getPropertyValue(user));
        return new VertexiumUser(user);
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
        QueryResultsIterable<Vertex> userVertices = graph.query(authorizations)
                .has(VisalloProperties.CONCEPT_TYPE.getPropertyName(), userConceptId)
                .skip(skip)
                .limit(limit)
                .vertices();
        return new ConvertingIterable<Vertex, User>(userVertices) {
            @Override
            protected User convert(Vertex vertex) {
                return createFromVertex(vertex);
            }
        };
    }

    @Override
    public Iterable<User> findByStatus(int skip, int limit, UserStatus status) {
        QueryResultsIterable<Vertex> userVertices = graph.query(authorizations)
                .has(VisalloProperties.CONCEPT_TYPE.getPropertyName(), userConceptId)
                .has(UserVisalloProperties.STATUS.getPropertyName(), status.toString())
                .skip(skip)
                .limit(limit)
                .vertices();
        return new ConvertingIterable<Vertex, User>(userVertices) {
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
    protected User addUser(String username, String displayName, String emailAddress, String password) {
        username = formatUsername(username);
        displayName = displayName.trim();

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
        UserVisalloProperties.STATUS.setProperty(
                userBuilder,
                UserStatus.OFFLINE.toString(),
                VISIBILITY.getVisibility()
        );

        if (emailAddress != null) {
            UserVisalloProperties.EMAIL_ADDRESS.setProperty(userBuilder, emailAddress, VISIBILITY.getVisibility());
        }

        User user = createFromVertex(userBuilder.save(this.authorizations));
        graph.flush();

        afterNewUserAdded(user);

        return user;
    }

    @Override
    public void setPassword(User user, byte[] salt, byte[] passwordHash) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserVisalloProperties.PASSWORD_SALT.setProperty(userVertex, salt, VISIBILITY.getVisibility(), authorizations);
        UserVisalloProperties.PASSWORD_HASH.setProperty(
                userVertex,
                passwordHash,
                VISIBILITY.getVisibility(),
                authorizations
        );
        graph.flush();
    }

    @Override
    public boolean isPasswordValid(User user, String password) {
        try {
            Vertex userVertex = findByIdUserVertex(user.getUserId());
            return UserPasswordUtil.validatePassword(
                    password,
                    UserVisalloProperties.PASSWORD_SALT.getPropertyValue(userVertex),
                    UserVisalloProperties.PASSWORD_HASH.getPropertyValue(userVertex)
            );
        } catch (Exception ex) {
            throw new RuntimeException("error validating password", ex);
        }
    }

    @Override
    public void updateUser(User user, AuthorizationContext authorizationContext) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        ExistingElementMutation<Vertex> m = userVertex.prepareMutation();

        Date currentLoginDate = UserVisalloProperties.CURRENT_LOGIN_DATE.getPropertyValue(userVertex);
        if (currentLoginDate != null) {
            UserVisalloProperties.PREVIOUS_LOGIN_DATE.setProperty(m, currentLoginDate, VISIBILITY.getVisibility());
        }

        String currentLoginRemoteAddr = UserVisalloProperties.CURRENT_LOGIN_REMOTE_ADDR.getPropertyValue(userVertex);
        if (currentLoginRemoteAddr != null) {
            UserVisalloProperties.PREVIOUS_LOGIN_REMOTE_ADDR.setProperty(
                    m,
                    currentLoginRemoteAddr,
                    VISIBILITY.getVisibility()
            );
        }

        UserVisalloProperties.CURRENT_LOGIN_DATE.setProperty(m, new Date(), VISIBILITY.getVisibility());
        UserVisalloProperties.CURRENT_LOGIN_REMOTE_ADDR.setProperty(
                m,
                authorizationContext.getRemoteAddr(),
                VISIBILITY.getVisibility()
        );

        int loginCount = UserVisalloProperties.LOGIN_COUNT.getPropertyValue(userVertex, 0);
        UserVisalloProperties.LOGIN_COUNT.setProperty(m, loginCount + 1, VISIBILITY.getVisibility());

        m.save(authorizations);
        graph.flush();

        getAuthorizationRepository().updateUser(user, authorizationContext);
        getPrivilegeRepository().updateUser(user, authorizationContext);
        fireUserLoginEvent(user, authorizationContext);
    }

    @Override
    public User setCurrentWorkspace(String userId, String workspaceId) {
        User user = findById(userId);
        checkNotNull(user, "Could not find user: " + userId);
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserVisalloProperties.CURRENT_WORKSPACE.setProperty(
                userVertex,
                workspaceId,
                VISIBILITY.getVisibility(),
                authorizations
        );
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
        UserVisalloProperties.UI_PREFERENCES.setProperty(
                userVertex,
                preferences,
                VISIBILITY.getVisibility(),
                authorizations
        );
        graph.flush();
    }

    @Override
    public User setStatus(String userId, UserStatus status) {
        VertexiumUser user = (VertexiumUser) findById(userId);
        checkNotNull(user, "Could not find user: " + userId);
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserVisalloProperties.STATUS.setProperty(
                userVertex,
                status.toString(),
                VISIBILITY.getVisibility(),
                authorizations
        );
        graph.flush();
        user.setUserStatus(status);
        fireUserStatusChangeEvent(user, status);
        return user;
    }

    @Override
    public void setDisplayName(User user, String displayName) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserVisalloProperties.DISPLAY_NAME.setProperty(
                userVertex,
                displayName,
                VISIBILITY.getVisibility(),
                authorizations
        );
        graph.flush();
    }

    @Override
    public void setEmailAddress(User user, String emailAddress) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserVisalloProperties.EMAIL_ADDRESS.setProperty(
                userVertex,
                emailAddress,
                VISIBILITY.getVisibility(),
                authorizations
        );
        graph.flush();
    }

    @Override
    protected void internalDelete(User user) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        graph.softDeleteVertex(userVertex, authorizations);
        graph.flush();
    }

    @Override
    public User findByPasswordResetToken(String token) {
        QueryResultsIterable<Vertex> userVertices = graph.query(authorizations)
                .has(UserVisalloProperties.PASSWORD_RESET_TOKEN.getPropertyName(), token)
                .has(VisalloProperties.CONCEPT_TYPE.getPropertyName(), userConceptId)
                .vertices();
        Vertex user = singleOrDefault(userVertices, null);
        return createFromVertex(user);
    }

    @Override
    public void setPasswordResetTokenAndExpirationDate(User user, String token, Date expirationDate) {
        Vertex userVertex = findByIdUserVertex(user.getUserId());
        UserVisalloProperties.PASSWORD_RESET_TOKEN.setProperty(
                userVertex,
                token,
                VISIBILITY.getVisibility(),
                authorizations
        );
        UserVisalloProperties.PASSWORD_RESET_TOKEN_EXPIRATION_DATE.setProperty(
                userVertex,
                expirationDate,
                VISIBILITY.getVisibility(),
                authorizations
        );
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
        if (!value.equals(user.getCustomProperties().get(propertyName))) {
            Vertex userVertex = findByIdUserVertex(user.getUserId());
            userVertex.setProperty(propertyName, value, VISIBILITY.getVisibility(), authorizations);
            if (user instanceof VertexiumUser) {
                ((VertexiumUser) user).setProperty(propertyName, value);
            }
            graph.flush();
        }
    }
}
