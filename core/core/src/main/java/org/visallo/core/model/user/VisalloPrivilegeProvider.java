package org.visallo.core.model.user;

import org.visallo.web.clientapi.model.Privilege;

public class VisalloPrivilegeProvider implements PrivilegesProvider {
    @Override
    public Iterable<Privilege> getPrivileges() {
        return Privilege.ALL_BUILT_IN;
    }
}
