package org.visallo.web.privilegeFilters;

import com.google.inject.Inject;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.clientapi.model.Privilege;

public class ReadPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected ReadPrivilegeFilter(UserRepository userRepository, PrivilegeRepository privilegeRepository) {
        super(Privilege.newSet(Privilege.READ), userRepository, privilegeRepository);
    }
}
