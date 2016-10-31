package org.visallo.core.model.user;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.json.JSONObject;
import org.vertexium.Graph;
import org.vertexium.TextIndexHint;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.notification.ExpirationAge;
import org.visallo.core.model.notification.UserNotification;
import org.visallo.core.model.notification.UserNotificationRepository;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyPropertyDefinition;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.user.SystemUser;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.PropertyType;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class UserPropertyAuthorizationRepositoryBase extends AuthorizationRepositoryBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(UserPropertyAuthorizationRepositoryBase.class);
    public static final String AUTHORIZATIONS_PROPERTY_IRI = "http://visallo.org/user#authorizations";
    private static final String SEPARATOR = ",";
    private final Configuration configuration;
    private final UserNotificationRepository userNotificationRepository;
    private final WorkQueueRepository workQueueRepository;
    private Collection<UserListener> userListeners;
    private GraphAuthorizationRepository authorizationRepository;

    protected UserPropertyAuthorizationRepositoryBase(
            Graph graph,
            OntologyRepository ontologyRepository,
            Configuration configuration,
            UserNotificationRepository userNotificationRepository,
            WorkQueueRepository workQueueRepository,
            GraphAuthorizationRepository authorizationRepository
    ) {
        super(graph);
        this.configuration = configuration;
        this.userNotificationRepository = userNotificationRepository;
        this.workQueueRepository = workQueueRepository;
        this.authorizationRepository = authorizationRepository;
        defineAuthorizationsProperty(ontologyRepository);
    }

    protected void defineAuthorizationsProperty(OntologyRepository ontologyRepository) {
        List<Concept> concepts = new ArrayList<>();
        concepts.add(ontologyRepository.getConceptByIRI(UserRepository.USER_CONCEPT_IRI));
        OntologyPropertyDefinition propertyDefinition = new OntologyPropertyDefinition(
                concepts,
                AUTHORIZATIONS_PROPERTY_IRI,
                "Authorizations",
                PropertyType.STRING
        );
        propertyDefinition.setUserVisible(false);
        propertyDefinition.setTextIndexHints(TextIndexHint.NONE);
        ontologyRepository.getOrCreateProperty(propertyDefinition);
    }

    protected ImmutableSet<String> parseAuthorizations(String authorizations) {
        checkNotNull(authorizations, "authorizations cannot be null");
        List<String> auths = Lists.newArrayList(authorizations.split(SEPARATOR)).stream()
                .map(String::trim)
                .filter(a -> a.length() > 0)
                .collect(Collectors.toList());
        return ImmutableSet.copyOf(auths);
    }

    @Override
    public void updateUser(User user, AuthorizationContext authorizationContext) {
    }

    @Override
    public Set<String> getAuthorizations(User user) {
        if (user instanceof SystemUser) {
            return Sets.newHashSet(VisalloVisibility.SUPER_USER_VISIBILITY_STRING);
        }
        HashSet<String> authSet = new HashSet<>(getDefaultAuthorizations());
        String authorizations = (String) user.getProperty(AUTHORIZATIONS_PROPERTY_IRI);
        if (authorizations != null) {
            authSet.addAll(parseAuthorizations(authorizations));
        }
        return authSet;
    }

    protected ImmutableSet<String> getDefaultAuthorizations() {
        return ImmutableSet.of();
    }

    protected void addAuthorization(User user, String auth, User authUser) {
        Set<String> auths = getAuthorizations(user);
        if (!auths.contains(auth)) {
            LOGGER.info(
                    "Adding authorization '%s' to user '%s' by '%s'",
                    auth,
                    user.getUsername(),
                    authUser.getUsername()
            );
            auths.add(auth);
            authorizationRepository.addAuthorizationToGraph(auth);
            getUserRepository().setPropertyOnUser(user, AUTHORIZATIONS_PROPERTY_IRI, Joiner.on(SEPARATOR).join(auths));
            sendNotificationToUserAboutAddAuthorization(user, auth, authUser);
            fireUserAddAuthorizationEvent(user, auth);
        }
    }

    protected void removeAuthorization(User user, String auth, User authUser) {
        Set<String> auths = getAuthorizations(user);
        if (auths.contains(auth)) {
            LOGGER.info(
                    "Removing authorization '%s' to user '%s' by '%s'",
                    auth,
                    user.getUsername(),
                    authUser.getUsername()
            );
            auths.remove(auth);
            getUserRepository().setPropertyOnUser(user, AUTHORIZATIONS_PROPERTY_IRI, Joiner.on(SEPARATOR).join(auths));
            sendNotificationToUserAboutRemoveAuthorization(user, auth, authUser);
            fireUserRemoveAuthorizationEvent(user, auth);
        }
    }

    protected void setAuthorizations(User user, Set<String> newAuthorizations, User authUser) {
        String[] newAuthorizationsArray = newAuthorizations.toArray(new String[newAuthorizations.size()]);
        authorizationRepository.addAuthorizationToGraph(newAuthorizationsArray);
        String newAuthorizationsString = Joiner.on(SEPARATOR).join(newAuthorizations);

        LOGGER.info(
                "Setting authorizations '%s' to user '%s' by '%s'",
                newAuthorizationsString,
                user.getUsername(),
                authUser.getUsername()
        );

        Set<String> currentAuthorizations = getAuthorizations(user);
        getUserRepository().setPropertyOnUser(user, AUTHORIZATIONS_PROPERTY_IRI, newAuthorizationsString);

        Set<String> addedAuthorizations = new HashSet<>(newAuthorizations);
        addedAuthorizations.removeAll(currentAuthorizations);
        for (String auth : addedAuthorizations) {
            sendNotificationToUserAboutAddAuthorization(user, auth, authUser);
            fireUserAddAuthorizationEvent(user, auth);
        }

        Set<String> removedAuthorizations = new HashSet<>(currentAuthorizations);
        removedAuthorizations.removeAll(newAuthorizations);
        for (String auth : removedAuthorizations) {
            sendNotificationToUserAboutRemoveAuthorization(user, auth, authUser);
            fireUserRemoveAuthorizationEvent(user, auth);
        }
    }

    private void sendNotificationToUserAboutAddAuthorization(User user, String auth, User authUser) {
        String title = "Authorization Added";
        String message = "Authorization Added: " + auth;
        String actionEvent = null;
        JSONObject actionPayload = null;
        ExpirationAge expirationAge = null;
        UserNotification userNotification = userNotificationRepository.createNotification(
                user.getUserId(),
                title,
                message,
                actionEvent,
                actionPayload,
                expirationAge,
                authUser
        );
        workQueueRepository.pushUserNotification(userNotification);
    }

    private void sendNotificationToUserAboutRemoveAuthorization(User user, String auth, User authUser) {
        String title = "Authorization Removed";
        String message = "Authorization Removed: " + auth;
        String actionEvent = null;
        JSONObject actionPayload = null;
        ExpirationAge expirationAge = null;
        UserNotification userNotification = userNotificationRepository.createNotification(
                user.getUserId(),
                title,
                message,
                actionEvent,
                actionPayload,
                expirationAge,
                authUser
        );
        workQueueRepository.pushUserNotification(userNotification);
    }

    private void fireUserAddAuthorizationEvent(User user, String auth) {
        for (UserListener userListener : getUserListeners()) {
            userListener.userAddAuthorization(user, auth);
        }
    }

    private void fireUserRemoveAuthorizationEvent(User user, String auth) {
        for (UserListener userListener : getUserListeners()) {
            userListener.userRemoveAuthorization(user, auth);
        }
    }

    private Collection<UserListener> getUserListeners() {
        if (userListeners == null) {
            userListeners = InjectHelper.getInjectedServices(UserListener.class, configuration);
        }
        return userListeners;
    }
}
