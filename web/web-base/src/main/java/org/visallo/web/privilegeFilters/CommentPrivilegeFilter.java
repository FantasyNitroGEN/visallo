package org.visallo.web.privilegeFilters;

import com.google.inject.Inject;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.clientapi.model.Privilege;

public class CommentPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected CommentPrivilegeFilter(UserRepository userRepository) {
        super(Privilege.newSet(Privilege.COMMENT), userRepository);
    }
}
