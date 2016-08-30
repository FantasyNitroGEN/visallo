package org.visallo.core.model.user;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import org.vertexium.Graph;
import org.visallo.core.config.Configurable;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.notification.UserNotificationRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.cli.AuthorizationRepositoryCliService;
import org.visallo.core.model.user.cli.AuthorizationRepositoryWithCliSupport;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.user.User;

import java.util.Set;

public class UserPropertyAuthorizationRepository extends UserPropertyAuthorizationRepositoryBase
        implements AuthorizationRepositoryWithCliSupport, UpdatableAuthorizationRepository {
    public static final String CONFIGURATION_PREFIX = UserPropertyAuthorizationRepository.class.getName();
    private final ImmutableSet<String> defaultAuthorizations;

    private static class Settings {
        @Configurable()
        public String defaultAuthorizations;
    }

    @Inject
    public UserPropertyAuthorizationRepository(
            Graph graph,
            OntologyRepository ontologyRepository,
            Configuration configuration,
            UserNotificationRepository userNotificationRepository,
            WorkQueueRepository workQueueRepository,
            GraphAuthorizationRepository authorizationRepository
    ) {
        super(
                graph,
                ontologyRepository,
                configuration,
                userNotificationRepository,
                workQueueRepository,
                authorizationRepository
        );

        Settings settings = new Settings();
        configuration.setConfigurables(settings, CONFIGURATION_PREFIX);
        this.defaultAuthorizations = parseAuthorizations(settings.defaultAuthorizations);
        if (settings.defaultAuthorizations.length() > 0) {
            String[] defaultAuthsArray = this.defaultAuthorizations.toArray(new String[this.defaultAuthorizations.size()]);
            authorizationRepository.addAuthorizationToGraph(defaultAuthsArray);
        }
    }

    @Override
    public AuthorizationRepositoryCliService getCliService() {
        return new UserPropertyAuthorizationRepositoryCliService(this);
    }

    @Override
    public ImmutableSet<String> getDefaultAuthorizations() {
        return defaultAuthorizations;
    }

    @Override
    public void addAuthorization(User user, String auth, User authUser) {
        super.addAuthorization(user, auth, authUser);
    }

    @Override
    public void removeAuthorization(User user, String auth, User authUser) {
        super.removeAuthorization(user, auth, authUser);
    }

    @Override
    public void setAuthorizations(User user, Set<String> newAuthorizations, User authUser) {
        super.setAuthorizations(user, newAuthorizations, authUser);
    }
}
