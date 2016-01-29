package org.visallo.web.privilegeFilters;

import com.google.inject.Inject;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.clientapi.model.Privilege;

public class ReadPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected ReadPrivilegeFilter(final UserRepository userRepository) {
        super(Privilege.newSet(Privilege.READ), userRepository);
    }
}
