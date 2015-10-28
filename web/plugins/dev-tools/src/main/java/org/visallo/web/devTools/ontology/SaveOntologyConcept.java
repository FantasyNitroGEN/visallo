package org.visallo.web.devTools.ontology;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONArray;
import org.vertexium.Authorizations;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperties;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.StringArrayUtil;
import org.visallo.web.VisalloResponse;

import java.util.Arrays;
import java.util.HashSet;

public class SaveOntologyConcept implements ParameterizedHandler {
    private OntologyRepository ontologyRepository;

    @Inject
    public SaveOntologyConcept(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Handle
    public void handle(
            @Optional(name = "parentIRI") String parentIRI,
            @Required(name = "concept") String conceptIRI,
            @Required(name = "displayName") String displayName,
            @Required(name = "color") String color,
            @Required(name = "displayType") String displayType,
            @Required(name = "titleFormula") String titleFormula,
            @Required(name = "subtitleFormula") String subtitleFormula,
            @Required(name = "timeFormula") String timeFormula,
            @Required(name = "addRelatedConceptWhiteList[]") String[] addRelatedConceptWhiteListArg,
            @Required(name = "intents[]") String[] intents,
            @Optional(name = "searchable", defaultValue = "true") boolean searchable,
            @Optional(name = "addable", defaultValue = "true") boolean addable,
            @Optional(name = "userVisible", defaultValue = "true") boolean userVisible,
            User user,
            Authorizations authorizations,
            VisalloResponse response
    ) throws Exception {
        HashSet<String> addRelatedConceptWhiteList = new HashSet<>(Arrays.asList(addRelatedConceptWhiteListArg));

        Concept concept = ontologyRepository.getConceptByIRI(conceptIRI);
        if (concept == null) {
            if (parentIRI == null) {
                throw new VisalloResourceNotFoundException("You must specify a parentIRI if you are creating a concept");
            }
            Concept parent = ontologyRepository.getConceptByIRI(parentIRI);
            if (parent == null) {
                throw new VisalloResourceNotFoundException("Could not find parent with iri: " + parentIRI);
            }
            concept = ontologyRepository.getOrCreateConcept(parent, conceptIRI, displayName, null);
        }

        if (displayName.length() != 0) {
            concept.setProperty(OntologyProperties.DISPLAY_NAME.getPropertyName(), displayName, authorizations);
        }

        if (color.length() != 0) {
            concept.setProperty(OntologyProperties.COLOR.getPropertyName(), color, authorizations);
        }

        JSONArray whiteList = new JSONArray();
        for (String whitelistIri : addRelatedConceptWhiteList) {
            whiteList.put(whitelistIri);
        }
        concept.setProperty(OntologyProperties.ADD_RELATED_CONCEPT_WHITE_LIST.getPropertyName(), whiteList.toString(), authorizations);

        concept.setProperty(OntologyProperties.DISPLAY_TYPE.getPropertyName(), displayType, authorizations);
        concept.setProperty(OntologyProperties.SEARCHABLE.getPropertyName(), searchable, authorizations);
        concept.setProperty(OntologyProperties.ADDABLE.getPropertyName(), addable, authorizations);
        concept.setProperty(OntologyProperties.USER_VISIBLE.getPropertyName(), userVisible, authorizations);

        if (titleFormula.length() != 0) {
            concept.setProperty(OntologyProperties.TITLE_FORMULA.getPropertyName(), titleFormula, authorizations);
        } else {
            concept.removeProperty(OntologyProperties.TITLE_FORMULA.getPropertyName(), authorizations);
        }

        if (subtitleFormula.length() != 0) {
            concept.setProperty(OntologyProperties.SUBTITLE_FORMULA.getPropertyName(), subtitleFormula, authorizations);
        } else {
            concept.removeProperty(OntologyProperties.SUBTITLE_FORMULA.getPropertyName(), authorizations);
        }

        if (timeFormula.length() != 0) {
            concept.setProperty(OntologyProperties.TIME_FORMULA.getPropertyName(), timeFormula, authorizations);
        } else {
            concept.removeProperty(OntologyProperties.TIME_FORMULA.getPropertyName(), authorizations);
        }

        concept.updateIntents(StringArrayUtil.removeNullOrEmptyElements(intents), authorizations);

        ontologyRepository.clearCache();

        response.respondWithSuccessJson();
    }
}
