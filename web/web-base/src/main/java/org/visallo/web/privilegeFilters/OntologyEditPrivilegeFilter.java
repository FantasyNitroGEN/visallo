package org.visallo.web.privilegeFilters;

import com.google.inject.Inject;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.clientapi.model.Privilege;

public class OntologyEditPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected OntologyEditPrivilegeFilter(final UserRepository userRepository, PrivilegeRepository privilegeRepository) {
        super(Privilege.newSet(Privilege.ONTOLOGY_ADD), userRepository, privilegeRepository);
    }
}
