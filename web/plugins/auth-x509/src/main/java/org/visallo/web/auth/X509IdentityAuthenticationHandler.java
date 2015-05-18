package org.visallo.web.auth;

import com.google.inject.Inject;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.X509AuthenticationHandler;
import org.vertexium.Graph;

public class X509IdentityAuthenticationHandler extends X509AuthenticationHandler {
    // default behavior is all that's needed

    @Inject
    public X509IdentityAuthenticationHandler(UserRepository userRepository, Graph graph) {
        super(userRepository, graph);
    }
}
