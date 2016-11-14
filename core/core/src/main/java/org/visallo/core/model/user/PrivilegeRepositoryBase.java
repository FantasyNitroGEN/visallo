package org.visallo.core.model.user;

import com.google.common.annotations.VisibleForTesting;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.Privilege;

import java.util.HashSet;
import java.util.Set;

public abstract class PrivilegeRepositoryBase implements PrivilegeRepository {
    private final Iterable<PrivilegesProvider> privilegesProviders;
    private UserRepository userRepository;

    protected PrivilegeRepositoryBase(Configuration configuration) {
        this.privilegesProviders = getPrivilegesProviders(configuration);
    }

    protected Iterable<PrivilegesProvider> getPrivilegesProviders(Configuration configuration) {
        return InjectHelper.getInjectedServices(PrivilegesProvider.class, configuration);
    }

    public boolean hasPrivilege(User user, String privilege) {
        Set<String> privileges = getPrivileges(user);
        return PrivilegeRepository.hasPrivilege(privileges, privilege);
    }

    public boolean hasAllPrivileges(User user, Set<String> requiredPrivileges) {
        return Privilege.hasAll(getPrivileges(user), requiredPrivileges);
    }

    // Need to late bind since UserRepository injects AuthorizationRepository in constructor
    protected UserRepository getUserRepository() {
        if (userRepository == null) {
            userRepository = InjectHelper.getInstance(UserRepository.class);
        }
        return userRepository;
    }

    @VisibleForTesting
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public abstract void updateUser(User user, AuthorizationContext authorizationContext);

    @Override
    public abstract Set<String> getPrivileges(User user);

    @Override
    public Set<Privilege> getAllPrivileges() {
        Set<Privilege> privileges = new HashSet<>();
        for (PrivilegesProvider privilegesProvider : privilegesProviders) {
            for (Privilege privilege : privilegesProvider.getPrivileges()) {
                privileges.add(privilege);
            }
        }
        return privileges;
    }

    protected Privilege findPrivilegeByName(String privilegeName) {
        for (Privilege p : getAllPrivileges()) {
            if (p.getName().equals(privilegeName)) {
                return p;
            }
        }
        return null;
    }
}
