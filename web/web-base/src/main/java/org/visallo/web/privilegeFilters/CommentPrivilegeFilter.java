package org.visallo.web.privilegeFilters;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.web.clientapi.model.Privilege;

import java.util.EnumSet;

public class CommentPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected CommentPrivilegeFilter(UserRepository userRepository) {
        super(EnumSet.of(Privilege.COMMENT), userRepository);
    }
}
