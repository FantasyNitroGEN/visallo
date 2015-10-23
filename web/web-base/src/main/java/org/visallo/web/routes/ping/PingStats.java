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

    @Inject
    public PingStats(
            UserRepository userRepository,
            Graph graph,
            AuthorizationRepository authorizationRepository
    ) {
        this.userRepository = userRepository;
        this.graph = graph;

        PingUtil.setup(authorizationRepository, userRepository);
    }

    @Handle
    public JSONObject stats(
            User user
    ) {
        Authorizations authorizations = userRepository.getAuthorizations(user, PingUtil.VISIBILITY_STRING);

        JSONObject json = new JSONObject();
        int[] minutes = {1, 5, 15};
        for (int i : minutes) {
            json.put(Integer.toString(i), PingUtil.getAverages(i, graph, authorizations));
        }
        return json;
    }
}
