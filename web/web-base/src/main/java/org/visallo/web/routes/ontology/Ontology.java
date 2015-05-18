package org.visallo.web.routes.ontology;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import com.v5analytics.webster.HandlerChain;
import org.visallo.web.BaseRequestHandler;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.util.ObjectMapperFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Ontology extends BaseRequestHandler {
    private final OntologyRepository ontologyRepository;

    @Inject
    public Ontology(
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        ClientApiOntology result = ontologyRepository.getClientApiObject();

        String json = ObjectMapperFactory.getInstance().writeValueAsString(result);
        String eTag = generateETag(json.getBytes());
        if (testEtagHeaders(request, response, eTag)) {
            return;
        }

        addETagHeader(response, eTag);
        respondWithClientApiObject(response, result);
    }
}
