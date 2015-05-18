package org.visallo.web.devTools.ontology;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperties;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.BaseRequestHandler;
import org.json.JSONArray;
import org.vertexium.Authorizations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;

public class SaveOntologyConcept extends BaseRequestHandler {
    private OntologyRepository ontologyRepository;

    @Inject
    public SaveOntologyConcept(UserRepository userRepository, WorkspaceRepository workspaceRepository, Configuration configuration, OntologyRepository ontologyRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain handlerChain) throws Exception {
        String conceptIRI = getRequiredParameter(request, "concept");
        String displayName = getRequiredParameter(request, "displayName");
        String color = getRequiredParameter(request, "color");
        String displayType = getRequiredParameter(request, "displayType");
        HashSet<String> addRelatedConceptWhiteList = new HashSet<String>(Arrays.asList(getRequiredParameterArray(request, "addRelatedConceptWhiteList[]")));
        Boolean searchable = getOptionalParameterBoolean(request, "searchable", true);
        Boolean addable = getOptionalParameterBoolean(request, "addable", true);
        Boolean userVisible = getOptionalParameterBoolean(request, "userVisible", true);
        String titleFormula = getRequiredParameter(request, "titleFormula");
        String subtitleFormula = getRequiredParameter(request, "subtitleFormula");
        String timeFormula = getRequiredParameter(request, "timeFormula");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        Concept concept = ontologyRepository.getConceptByIRI(conceptIRI);
        if (concept == null) {
            respondWithNotFound(response, "concept " + conceptIRI + " not found");
            return;
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

        ontologyRepository.clearCache();

        respondWithHtml(response, "OK");
    }
}
