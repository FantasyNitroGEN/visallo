package org.visallo.core.model.user.cli;

import org.visallo.core.model.user.PrivilegeRepository;

public interface PrivilegeRepositoryWithCliSupport extends PrivilegeRepository {
    PrivilegeRepositoryCliService getCliService();
}
