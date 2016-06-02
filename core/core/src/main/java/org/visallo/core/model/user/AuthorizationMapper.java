package org.visallo.core.model.user;

import java.util.Set;

public abstract class AuthorizationMapper {
    public abstract Set<String> getPrivileges(AuthorizationContext authorizationContext);

    public abstract Set<String> getAuthorizations(AuthorizationContext authorizationContext);
}
