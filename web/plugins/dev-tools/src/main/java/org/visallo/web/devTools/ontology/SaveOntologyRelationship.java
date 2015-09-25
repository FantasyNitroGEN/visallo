package org.visallo.web.devTools.ontology;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.visallo.core.model.ontology.OntologyProperties;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.user.User;
import org.visallo.core.util.StringArrayUtil;
import org.visallo.web.VisalloResponse;

public class SaveOntologyRelationship implements ParameterizedHandler {
    private OntologyRepository ontologyRepository;

    @Inject
    public SaveOntologyRelationship(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Handle
    public void handle(
            @Required(name = "relationship") String relationshipIri,
            @Required(name = "displayName") String displayName,
            @Required(name = "intents[]") String[] intents,
            User user,
            Authorizations authorizations,
            VisalloResponse response
    ) throws Exception {
        Relationship relationship = ontologyRepository.getRelationshipByIRI(relationshipIri);
        if (relationship == null) {
            response.respondWithNotFound("relationship " + relationshipIri + " not found");
            return;
        }

        if (displayName.length() != 0) {
            relationship.setProperty(OntologyProperties.DISPLAY_NAME.getPropertyName(), displayName, authorizations);
        }

        relationship.updateIntents(StringArrayUtil.removeNullOrEmptyElements(intents), authorizations);

        ontologyRepository.clearCache();

        response.respondWithSuccessJson();
    }
}
