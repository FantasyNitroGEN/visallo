package org.visallo.web.devTools.ontology;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.TextIndexHint;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.ontology.*;
import org.visallo.core.user.User;
import org.visallo.core.util.JSONUtil;
import org.visallo.core.util.StringArrayUtil;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.PropertyType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SaveOntologyProperty implements ParameterizedHandler {
    private OntologyRepository ontologyRepository;

    @Inject
    public SaveOntologyProperty(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Handle
    public void handle(
            @Required(name = "property") String propertyIri,
            @Required(name = "displayName") String displayName,
            @Required(name = "dataType") String dataTypeString,
            @Required(name = "displayType") String displayType,
            @Required(name = "displayFormula") String displayFormula,
            @Required(name = "validationFormula") String validationFormula,
            @Required(name = "possibleValues") String possibleValues,
            @Required(name = "dependentPropertyIris[]") String[] dependentPropertyIrisArg,
            @Required(name = "intents[]") String[] intents,
            @Optional(name = "concepts[]") String[] conceptIris,
            @Optional(name = "domains[]") String[] domains,
            @Optional(name = "searchable", defaultValue = "true") boolean searchable,
            @Optional(name = "textIndexHints") String textIndexHints,
            @Optional(name = "addable", defaultValue = "true") boolean addable,
            @Optional(name = "sortable", defaultValue = "true") boolean sortable,
            @Optional(name = "userVisible", defaultValue = "true") boolean userVisible,
            @Optional(name = "deleteable", defaultValue = "true") boolean deleteable,
            @Optional(name = "updateable", defaultValue = "true") boolean updateable,
            User user,
            Authorizations authorizations,
            VisalloResponse response
    ) throws Exception {
        PropertyType dataType = PropertyType.convert(dataTypeString);

        OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyIri);
        if (property == null) {
            if (conceptIris == null || conceptIris.length == 0) {
                throw new VisalloException("You must specify at least one concept if you are creating a property");
            }
            List<Concept> concepts = Lists.newArrayList(Iterables.transform(Arrays.asList(conceptIris), new Function<String, Concept>() {
                @Override
                public Concept apply(String conceptIri) {
                    Concept concept = ontologyRepository.getConceptByIRI(conceptIri);
                    if (concept == null) {
                        throw new VisalloResourceNotFoundException("Could not find concept with IRI '" + conceptIri + "'");
                    }
                    return concept;
                }
            }));
            OntologyPropertyDefinition propertyDefinition = new OntologyPropertyDefinition(
                    concepts,
                    propertyIri,
                    displayName,
                    dataType
            );
            propertyDefinition.setSearchable(searchable);
            if (searchable && dataType == PropertyType.STRING) {
                if (textIndexHints == null || textIndexHints.length() == 0) {
                    throw new VisalloException("textIndexHints are required for searchable strings");
                }
                propertyDefinition.setTextIndexHints(TextIndexHint.parse(textIndexHints));
            }
            propertyDefinition.setAddable(addable);
            propertyDefinition.setSortable(sortable);
            propertyDefinition.setUserVisible(userVisible);
            propertyDefinition.setDeleteable(deleteable);
            propertyDefinition.setUpdateable(updateable);
            property = ontologyRepository.getOrCreateProperty(propertyDefinition);
        }

        if (displayName.length() != 0) {
            property.setProperty(OntologyProperties.DISPLAY_NAME.getPropertyName(), displayName, authorizations);
        }

        ArrayList<String> dependentPropertyIris = Lists.newArrayList(Iterables.filter(Arrays.asList(dependentPropertyIrisArg), new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input.trim().length() > 0;
            }
        }));
        ontologyRepository.updatePropertyDependentIris(property, dependentPropertyIris);

        property.setProperty(OntologyProperties.DISPLAY_TYPE.getPropertyName(), displayType, authorizations);
        property.setProperty(OntologyProperties.DATA_TYPE.getPropertyName(), dataType.toString(), authorizations);
        property.setProperty(OntologyProperties.SEARCHABLE.getPropertyName(), searchable, authorizations);
        property.setProperty(OntologyProperties.SORTABLE.getPropertyName(), sortable, authorizations);
        property.setProperty(OntologyProperties.ADDABLE.getPropertyName(), addable, authorizations);
        property.setProperty(OntologyProperties.USER_VISIBLE.getPropertyName(), userVisible, authorizations);
        property.setProperty(OntologyProperties.DELETEABLE.getPropertyName(), deleteable, authorizations);
        property.setProperty(OntologyProperties.UPDATEABLE.getPropertyName(), updateable, authorizations);
        if (possibleValues != null && possibleValues.trim().length() > 0) {
            possibleValues = JSONUtil.parse(possibleValues).toString();
            property.setProperty(OntologyProperties.POSSIBLE_VALUES.getPropertyName(), possibleValues, authorizations);
        }

        property.setProperty(OntologyProperties.DISPLAY_FORMULA.getPropertyName(), displayFormula, authorizations);
        property.setProperty(OntologyProperties.VALIDATION_FORMULA.getPropertyName(), validationFormula, authorizations);

        property.updateIntents(StringArrayUtil.removeNullOrEmptyElements(intents), authorizations);

        ontologyRepository.updatePropertyDomainIris(property, Sets.newHashSet(domains));

        ontologyRepository.clearCache();

        response.respondWithSuccessJson();
    }
}
