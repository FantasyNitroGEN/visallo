package org.visallo.vertexium.model.user;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.security.Authorizations;
import org.vertexium.Graph;
import org.vertexium.accumulo.AccumuloGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AccumuloAuthorizationRepository implements AuthorizationRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(AccumuloAuthorizationRepository.class);
    public static final String LOCK_NAME = AccumuloAuthorizationRepository.class.getName();
    private Graph graph;
    private LockRepository lockRepository;

    public void addAuthorizationToGraph(final String... auths) {
        LOGGER.info("adding authorizations [%s] for vertexium user", Joiner.on(", ").join(auths));
        lockRepository.lock(LOCK_NAME, new Runnable() {
            @Override
            public void run() {
                LOGGER.debug("got lock to add authorizations [%s] for vertexium user", Joiner.on(", ").join(auths));
                if (graph instanceof AccumuloGraph) {
                    for (String auth : auths) {
                        LOGGER.debug("adding authorization [%s] for vertexium user", auth);
                        try {
                            AccumuloGraph accumuloGraph = (AccumuloGraph) graph;
                            String principal = accumuloGraph.getConnector().whoami();
                            Authorizations currentAuthorizations = accumuloGraph.getConnector().securityOperations().getUserAuthorizations(principal);
                            if (currentAuthorizations.contains(auth)) {
                                continue;
                            }
                            List<byte[]> newAuthorizationsArray = new ArrayList<>();
                            for (byte[] currentAuth : currentAuthorizations) {
                                newAuthorizationsArray.add(currentAuth);
                            }
                            newAuthorizationsArray.add(auth.getBytes(Constants.UTF8));
                            Authorizations newAuthorizations = new Authorizations(newAuthorizationsArray);
                            accumuloGraph.getConnector().securityOperations().changeUserAuthorizations(principal, newAuthorizations);
                        } catch (Exception ex) {
                            throw new VisalloException("Could not update authorizations in accumulo", ex);
                        }
                    }
                } else {
                    throw new VisalloException("graph type not supported to add authorizations.");
                }
            }
        });
    }

    public void removeAuthorizationFromGraph(final String auth) {
        LOGGER.info("removing authorization to graph user %s", auth);
        lockRepository.lock(LOCK_NAME, new Runnable() {
            @Override
            public void run() {
                LOGGER.debug("got lock removing authorization to graph user %s", auth);
                if (graph instanceof AccumuloGraph) {
                    try {
                        AccumuloGraph accumuloGraph = (AccumuloGraph) graph;
                        String principal = accumuloGraph.getConnector().whoami();
                        Authorizations currentAuthorizations = accumuloGraph.getConnector().securityOperations().getUserAuthorizations(principal);
                        if (!currentAuthorizations.toString().contains(auth)) {
                            return;
                        }
                        byte[] authBytes = auth.getBytes(Constants.UTF8);
                        List<byte[]> newAuthorizationsArray = new ArrayList<>();
                        for (byte[] currentAuth : currentAuthorizations) {
                            if (Arrays.equals(currentAuth, authBytes)) {
                                continue;
                            }
                            newAuthorizationsArray.add(currentAuth);
                        }
                        Authorizations newAuthorizations = new Authorizations(newAuthorizationsArray);
                        accumuloGraph.getConnector().securityOperations().changeUserAuthorizations(principal, newAuthorizations);
                    } catch (Exception ex) {
                        throw new RuntimeException("Could not update authorizations in accumulo", ex);
                    }
                } else {
                    throw new RuntimeException("graph type not supported to add authorizations.");
                }
            }
        });
    }

    @Override
    public List<String> getGraphAuthorizations() {
        if (graph instanceof AccumuloGraph) {
            try {
                AccumuloGraph accumuloGraph = (AccumuloGraph) graph;
                String principal = accumuloGraph.getConnector().whoami();
                Authorizations currentAuthorizations = accumuloGraph.getConnector().securityOperations().getUserAuthorizations(principal);
                ArrayList<String> auths = new ArrayList<>();
                for (byte[] currentAuth : currentAuthorizations) {
                    auths.add(new String(currentAuth));
                }
                return auths;
            } catch (Exception ex) {
                throw new RuntimeException("Could not get authorizations from accumulo", ex);
            }
        } else {
            throw new RuntimeException("graph type not supported to add authorizations.");
        }
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Inject
    public void setLockRepository(LockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }
}
