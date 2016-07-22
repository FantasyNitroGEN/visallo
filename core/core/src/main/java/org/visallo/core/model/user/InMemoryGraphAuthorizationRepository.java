package org.visallo.core.model.user;

import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.*;

public class InMemoryGraphAuthorizationRepository implements GraphAuthorizationRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(InMemoryGraphAuthorizationRepository.class);
    private List<String> authorizations = new ArrayList<>();

    @Override
    public void addAuthorizationToGraph(String... auths) {
        for (String auth : auths) {
            LOGGER.info("Adding authorization to graph user %s", auth);
            authorizations.add(auth);
        }
    }

    @Override
    public void removeAuthorizationFromGraph(String auth) {
        LOGGER.info("Removing authorization to graph user %s", auth);
        authorizations.remove(auth);
    }

    @Override
    public List<String> getGraphAuthorizations() {
        LOGGER.info("getting authorizations");
        return new ArrayList<>(authorizations);
    }
}
