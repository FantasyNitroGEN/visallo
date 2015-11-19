package org.visallo.web.devTools.ontology;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
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
            @Required(name = "titleFormula") String titleFormula,
            @Required(name = "subtitleFormula") String subtitleFormula,
            @Required(name = "timeFormula") String timeFormula,
            @Required(name = "intents[]") String[] intents,
            @Optional(name = "deleteable", defaultValue = "true") boolean deleteable,
            @Optional(name = "updateable", defaultValue = "true") boolean updateable,
            User user,
            Authorizations authorizations,
            VisalloResponse response
    ) throws Exception {
        Relationship relationship = ontologyRepository.getRelationshipByIRI(relationshipIri);
        if (relationship == null) {
            response.respondWithNotFound("relationship " + relationshipIri + " not found");
            return;
        }

        relationship.setProperty(OntologyProperties.UPDATEABLE.getPropertyName(), updateable, authorizations);
        relationship.setProperty(OntologyProperties.DELETEABLE.getPropertyName(), deleteable, authorizations);

        if (displayName.length() != 0) {
            relationship.setProperty(OntologyProperties.DISPLAY_NAME.getPropertyName(), displayName, authorizations);
        }

        if (titleFormula.length() != 0) {
            relationship.setProperty(OntologyProperties.TITLE_FORMULA.getPropertyName(), titleFormula, authorizations);
        } else {
            relationship.removeProperty(OntologyProperties.TITLE_FORMULA.getPropertyName(), authorizations);
        }

        if (subtitleFormula.length() != 0) {
            relationship.setProperty(OntologyProperties.SUBTITLE_FORMULA.getPropertyName(), subtitleFormula, authorizations);
        } else {
            relationship.removeProperty(OntologyProperties.SUBTITLE_FORMULA.getPropertyName(), authorizations);
        }

        if (timeFormula.length() != 0) {
            relationship.setProperty(OntologyProperties.TIME_FORMULA.getPropertyName(), timeFormula, authorizations);
        } else {
            relationship.removeProperty(OntologyProperties.TIME_FORMULA.getPropertyName(), authorizations);
        }

        relationship.updateIntents(StringArrayUtil.removeNullOrEmptyElements(intents), authorizations);

        ontologyRepository.clearCache();

        response.respondWithSuccessJson();
    }
}
