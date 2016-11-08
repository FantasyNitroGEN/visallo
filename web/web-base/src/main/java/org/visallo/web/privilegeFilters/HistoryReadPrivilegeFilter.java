package org.visallo.web.privilegeFilters;

import com.google.inject.Inject;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.clientapi.model.Privilege;

public class HistoryReadPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected HistoryReadPrivilegeFilter(UserRepository userRepository, PrivilegeRepository privilegeRepository) {
        super(Privilege.newSet(Privilege.READ, Privilege.HISTORY_READ), userRepository, privilegeRepository);
    }
}
