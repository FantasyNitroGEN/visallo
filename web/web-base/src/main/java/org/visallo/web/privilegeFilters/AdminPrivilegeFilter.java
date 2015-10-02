package org.visallo.web.privilegeFilters;

import com.google.inject.Inject;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.clientapi.model.Privilege;

import java.util.EnumSet;

public class AdminPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected AdminPrivilegeFilter(final UserRepository userRepository) {
        super(EnumSet.of(Privilege.ADMIN), userRepository);
    }
}
