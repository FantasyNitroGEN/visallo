package org.visallo.web.privilegeFilters;

import com.google.inject.Inject;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.clientapi.model.Privilege;

import java.util.EnumSet;

public class PublishPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected PublishPrivilegeFilter(final UserRepository userRepository) {
        super(EnumSet.of(Privilege.PUBLISH), userRepository);
    }
}
