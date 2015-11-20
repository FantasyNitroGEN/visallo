package org.visallo.core.model.user;

import com.google.inject.ImplementedBy;

/**
 * This interface provides methods to keep track of user sessions across a cluster.
 * Use {@link org.visallo.core.bootstrap.BootstrapBindingProvider} to bind to an implementation of
 * {@link UserSessionCounterRepository}.
 */
public interface UserSessionCounterRepository {
    /**
     * Create or update a user session.
     * @return the number of sessions for this user after updating
     */
    int updateSession(String userId, String sessionId, boolean autoDelete);

    /**
     * Delete all sessions for a user.
     */
    void deleteSessions(String userId);

    /**
     * Delete a user session.
     * @return the number of sessions for this user after deleting
     */
    int deleteSession(String userId, String sessionId);
}
