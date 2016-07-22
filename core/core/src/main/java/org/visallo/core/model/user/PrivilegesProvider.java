package org.visallo.core.model.user;

import org.visallo.web.clientapi.model.Privilege;

public interface PrivilegesProvider {
    Iterable<Privilege> getPrivileges();
}
