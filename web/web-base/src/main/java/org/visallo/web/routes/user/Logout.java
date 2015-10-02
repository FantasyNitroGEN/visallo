package org.visallo.web.routes.user;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.visallo.core.model.user.UserSessionCounterRepository;
import org.visallo.web.CurrentUser;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;

import javax.servlet.http.HttpServletRequest;

public class Logout implements ParameterizedHandler {
    private UserSessionCounterRepository userSessionCounterRepository;

    @Inject
    public Logout(final UserSessionCounterRepository userSessionCounterRepository) {
        this.userSessionCounterRepository = userSessionCounterRepository;
    }

    @Handle
    public ClientApiSuccess handle(HttpServletRequest request) throws Exception {
        String userId = CurrentUser.getUserId(request);
        String sessionId = request.getSession().getId();
        this.userSessionCounterRepository.deleteSession(userId, sessionId);
        CurrentUser.clearUserFromSession(request);
        request.getSession().invalidate();
        return VisalloResponse.SUCCESS;
    }
}
