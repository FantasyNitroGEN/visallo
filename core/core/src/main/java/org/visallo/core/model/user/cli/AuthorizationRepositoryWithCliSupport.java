package org.visallo.core.model.user.cli;

import org.visallo.core.model.user.AuthorizationRepository;

public interface AuthorizationRepositoryWithCliSupport extends AuthorizationRepository {
    AuthorizationRepositoryCliService getCliService();
}
