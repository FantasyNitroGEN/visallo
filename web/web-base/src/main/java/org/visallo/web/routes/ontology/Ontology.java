package org.visallo.web.routes.ontology;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import org.vertexium.Authorizations;
import org.vertexium.util.IterableUtils;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.util.ObjectMapperFactory;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.List;
import java.util.stream.Collectors;

public class Ontology implements ParameterizedHandler {
    private final OntologyRepository ontologyRepository;

    @Inject
    public Ontology(final OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Handle
    public ClientApiOntology handle(
            @ActiveWorkspaceId String workspaceId,
            User user,
            VisalloResponse response) throws Exception {

        ClientApiOntology result = ontologyRepository.getClientApiObject(user, workspaceId);

        String json = ObjectMapperFactory.getInstance().writeValueAsString(result);
        String eTag = response.generateETag(json.getBytes());
        if (response.testEtagHeaders(eTag)) {
            return result;
        }

        response.addETagHeader(eTag);
        return result;
    }
}
