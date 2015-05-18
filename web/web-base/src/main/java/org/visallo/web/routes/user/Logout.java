package org.visallo.web.routes.user;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.user.UserSessionCounterRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.CurrentUser;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Logout extends BaseRequestHandler {
    private UserSessionCounterRepository userSessionCounterRepository;

    @Inject
    public Logout(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration,
            final UserSessionCounterRepository userSessionCounterRepository
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.userSessionCounterRepository = userSessionCounterRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String userId = CurrentUser.get(request);
        String sessionId = request.getSession().getId();
        this.userSessionCounterRepository.deleteSession(userId, sessionId);
        CurrentUser.clear(request);
        request.getSession().invalidate();
        JSONObject json = new JSONObject();
        json.put("status", "ok");
        respondWithJson(response, json);
    }
}
