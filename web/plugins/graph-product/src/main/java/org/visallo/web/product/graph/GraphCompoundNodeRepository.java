package org.visallo.web.product.graph;


import com.google.inject.Inject;
import org.vertexium.Graph;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.GraphAuthorizationRepository;
import org.visallo.core.model.user.UserRepository;

public class GraphCompoundNodeRepository {
    private final Graph graph;
    private final UserRepository userRepository;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public GraphCompoundNodeRepository(
            Graph graph,
            UserRepository userRepository,
            GraphAuthorizationRepository graphAuthorizationRepository,
            AuthorizationRepository authorizationRepository
    ) {
        this.graph = graph;
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
    }


//    public ClientApiGraphCompoundNode toClientApi(String nodeId) {
//
//    }
}
