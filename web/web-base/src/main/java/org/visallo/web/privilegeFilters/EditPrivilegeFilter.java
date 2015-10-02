package org.visallo.web.privilegeFilters;

import com.google.inject.Inject;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.clientapi.model.Privilege;

import java.util.EnumSet;

public class EditPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected EditPrivilegeFilter(UserRepository userRepository) {
        super(EnumSet.of(Privilege.EDIT), userRepository);
    }
}
