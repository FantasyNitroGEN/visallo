package org.visallo.core.model.user;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import java.util.Set;

public class DefaultAuthorizationMapper extends AuthorizationMapper {
    private final UserRepository userRepository;

    @Inject
    public DefaultAuthorizationMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Set<String> getPrivileges(AuthorizationContext authorizationContext) {
        if (authorizationContext.isNewUser()) {
            return this.userRepository.getDefaultPrivileges();
        }
        return authorizationContext.getExistingUser().getPrivileges();
    }

    @Override
    public Set<String> getAuthorizations(AuthorizationContext authorizationContext) {
        if (authorizationContext.isNewUser()) {
            return this.userRepository.getDefaultAuthorizations();
        }
        return Sets.newHashSet(
                userRepository.getAuthorizations(authorizationContext.getExistingUser()).getAuthorizations()
        );
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }
}
