package org.visallo.core.model.ontology;

import com.google.common.collect.ImmutableList;
import org.vertexium.TextIndexHint;
import org.visallo.web.clientapi.model.PropertyType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OntologyPropertyDefinition {
    private final List<Concept> concepts;
    private final List<Relationship> relationships;
    private final String propertyIri;
    private final String displayName;
    private final PropertyType dataType;
    private Map<String, String> possibleValues;
    private Collection<TextIndexHint> textIndexHints;
    private boolean userVisible;
    private boolean searchable;
    private boolean addable;
    private boolean sortable;
    private String displayType;
    private String propertyGroup;
    private Double boost;
    private String validationFormula;
    private String displayFormula;
    private ImmutableList<String> dependentPropertyIris;
    private String[] intents;
    private boolean deleteable;
    private boolean updateable;

    public OntologyPropertyDefinition(
            List<Concept> concepts,
            String propertyIri,
            String displayName,
            PropertyType dataType
    ) {
        this.concepts = concepts;
        this.relationships = new ArrayList<>();
        this.propertyIri = propertyIri;
        this.displayName = displayName;
        this.dataType = dataType;
    }

    public List<Relationship> getRelationships() {
        return relationships;
    }

    public List<Concept> getConcepts() {
        return concepts;
    }

    public String getPropertyIri() {
        return propertyIri;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PropertyType getDataType() {
        return dataType;
    }

    public Map<String, String> getPossibleValues() {
        return possibleValues;
    }

    public OntologyPropertyDefinition setPossibleValues(Map<String, String> possibleValues) {
        this.possibleValues = possibleValues;
        return this;
    }

    public Collection<TextIndexHint> getTextIndexHints() {
        return textIndexHints;
    }

    public OntologyPropertyDefinition setTextIndexHints(Collection<TextIndexHint> textIndexHints) {
        this.textIndexHints = textIndexHints;
        return this;
    }

    public boolean isUserVisible() {
        return userVisible;
    }

    public OntologyPropertyDefinition setUserVisible(boolean userVisible) {
        this.userVisible = userVisible;
        return this;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public OntologyPropertyDefinition setSearchable(boolean searchable) {
        this.searchable = searchable;
        return this;
    }

    public boolean isAddable() {
        return addable;
    }

    public OntologyPropertyDefinition setAddable(boolean addable) {
        this.addable = addable;
        return this;
    }

    public boolean isSortable() {
        return sortable;
    }

    public OntologyPropertyDefinition setSortable(boolean sortable) {
        this.sortable = sortable;
        return this;
    }

    public String getDisplayType() {
        return displayType;
    }

    public OntologyPropertyDefinition setDisplayType(String displayType) {
        this.displayType = displayType;
        return this;
    }

    public String getPropertyGroup() {
        return propertyGroup;
    }

    public OntologyPropertyDefinition setPropertyGroup(String propertyGroup) {
        this.propertyGroup = propertyGroup;
        return this;
    }

    public Double getBoost() {
        return boost;
    }

    public OntologyPropertyDefinition setBoost(Double boost) {
        this.boost = boost;
        return this;
    }

    public String getValidationFormula() {
        return validationFormula;
    }

    public OntologyPropertyDefinition setValidationFormula(String validationFormula) {
        this.validationFormula = validationFormula;
        return this;
    }

    public String getDisplayFormula() {
        return displayFormula;
    }

    public OntologyPropertyDefinition setDisplayFormula(String displayFormula) {
        this.displayFormula = displayFormula;
        return this;
    }

    public ImmutableList<String> getDependentPropertyIris() {
        return dependentPropertyIris;
    }

    public OntologyPropertyDefinition setDependentPropertyIris(ImmutableList<String> dependentPropertyIris) {
        this.dependentPropertyIris = dependentPropertyIris;
        return this;
    }

    public String[] getIntents() {
        return intents;
    }

    public OntologyPropertyDefinition setIntents(String[] intents) {
        this.intents = intents;
        return this;
    }

    public boolean getDeleteable() {
        return deleteable;
    }

    public void setDeleteable(boolean deleteable) {
        this.deleteable = deleteable;
    }

    public boolean getUpdateable() {
        return updateable;
    }

    public void setUpdateable(boolean updateable) {
        this.updateable = updateable;
    }
}
