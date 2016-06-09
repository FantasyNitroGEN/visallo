package org.visallo.web.routes.ping;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.ping.PingUtil;
import org.visallo.core.user.User;

public class PingStats implements ParameterizedHandler {
    private final UserRepository userRepository;
    private final Graph graph;
    private final PingUtil pingUtil;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public PingStats(
            UserRepository userRepository,
            Graph graph,
            PingUtil pingUtil,
            AuthorizationRepository authorizationRepository
    ) {
        this.userRepository = userRepository;
        this.graph = graph;
        this.pingUtil = pingUtil;
        this.authorizationRepository = authorizationRepository;
    }

    @Handle
    public JSONObject stats(
            User user
    ) {
        Authorizations authorizations = authorizationRepository.getGraphAuthorizations(
                user,
                PingUtil.VISIBILITY_STRING
        );

        JSONObject json = new JSONObject();
        int[] minutes = {1, 5, 15};
        for (int i : minutes) {
            json.put(Integer.toString(i), pingUtil.getAverages(i, graph, authorizations));
        }
        return json;
    }
}
