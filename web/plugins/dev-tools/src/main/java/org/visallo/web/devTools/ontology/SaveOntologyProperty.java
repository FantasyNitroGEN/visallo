package org.visallo.web.devTools.ontology;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import org.json.JSONArray;
import org.mortbay.util.ajax.JSON;
import org.vertexium.Authorizations;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.ontology.OntologyProperties;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.BaseRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;

public class SaveOntologyProperty extends BaseRequestHandler {
    private OntologyRepository ontologyRepository;

    @Inject
    public SaveOntologyProperty(UserRepository userRepository, WorkspaceRepository workspaceRepository, Configuration configuration, OntologyRepository ontologyRepository) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain handlerChain) throws Exception {
        String propertyIri = getRequiredParameter(request, "property");
        String displayName = getRequiredParameter(request, "displayName");
        String dataType = getRequiredParameter(request, "dataType");
        String displayType = getRequiredParameter(request, "displayType");
        HashSet<String> dependentPropertyIris = new HashSet<>(Arrays.asList(getRequiredParameterArray(request, "dependentPropertyIris[]")));
        Boolean searchable = getOptionalParameterBoolean(request, "searchable", true);
        Boolean addable = getOptionalParameterBoolean(request, "addable", true);
        Boolean userVisible = getOptionalParameterBoolean(request, "userVisible", true);
        String displayFormula = getRequiredParameter(request, "displayFormula");
        String validationFormula = getRequiredParameter(request, "validationFormula");
        String possibleValues = getRequiredParameter(request, "possibleValues");

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyIri);
        if (property == null) {
            respondWithNotFound(response, "property " + propertyIri + " not found");
            return;
        }

        if (displayName.length() != 0) {
            property.setProperty(OntologyProperties.DISPLAY_NAME.getPropertyName(), displayName, authorizations);
        }

        JSONArray dependantIris = new JSONArray();
        for (String whitelistIri : dependentPropertyIris) {
            dependantIris.put(whitelistIri);
        }
        property.setProperty(OntologyProperties.EDGE_LABEL_DEPENDENT_PROPERTY, dependantIris.toString(), authorizations);

        property.setProperty(OntologyProperties.DISPLAY_TYPE.getPropertyName(), displayType, authorizations);
        property.setProperty(OntologyProperties.DATA_TYPE.getPropertyName(), dataType, authorizations);
        property.setProperty(OntologyProperties.SEARCHABLE.getPropertyName(), searchable, authorizations);
        property.setProperty(OntologyProperties.ADDABLE.getPropertyName(), addable, authorizations);
        property.setProperty(OntologyProperties.USER_VISIBLE.getPropertyName(), userVisible, authorizations);
        if (possibleValues != null && possibleValues.trim().length() > 0) {
            possibleValues = JSON.toString(JSON.parse(possibleValues));
            property.setProperty(OntologyProperties.POSSIBLE_VALUES.getPropertyName(), possibleValues, authorizations);
        }

        property.setProperty(OntologyProperties.DISPLAY_FORMULA.getPropertyName(), displayFormula, authorizations);
        property.setProperty(OntologyProperties.VALIDATION_FORMULA.getPropertyName(), validationFormula, authorizations);

        ontologyRepository.clearCache();

        respondWithHtml(response, "OK");
    }
}
