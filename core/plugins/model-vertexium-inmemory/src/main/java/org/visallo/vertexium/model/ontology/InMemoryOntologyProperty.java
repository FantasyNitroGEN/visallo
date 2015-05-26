package org.visallo.vertexium.model.ontology;

import com.google.common.collect.ImmutableList;
import org.visallo.core.model.ontology.OntologyProperties;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.util.JSONUtil;
import org.visallo.web.clientapi.model.PropertyType;
import org.vertexium.Authorizations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InMemoryOntologyProperty extends OntologyProperty {
    private String title;
    private boolean userVisible;
    private boolean searchable;
    private boolean addable;
    private String displayName;
    private String propertyGroup;
    private PropertyType dataType;
    private Map<String, String> possibleValues;
    private String displayType;
    private Double boost;
    private String validationFormula;
    private String displayFormula;
    private ImmutableList<String> dependentPropertyIris = ImmutableList.<String>of();
    private List<String> intents = new ArrayList<>();

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean getUserVisible() {
        return userVisible;
    }

    @Override
    public PropertyType getDataType() {
        return dataType;
    }

    @Override
    public Double getBoost() {
        return boost;
    }

    @Override
    public Map<String, String> getPossibleValues() {
        return possibleValues;
    }

    @Override
    public String getPropertyGroup() {
        return propertyGroup;
    }

    @Override
    public boolean getSearchable() {
        return searchable;
    }

    @Override
    public boolean getAddable() {
        return addable;
    }

    @Override
    public String getValidationFormula() {
        return validationFormula;
    }

    @Override
    public String getDisplayFormula() {
        return displayFormula;
    }

    @Override
    public ImmutableList<String> getDependentPropertyIris() {
        return dependentPropertyIris;
    }

    public String[] getIntents() {
        return this.intents.toArray(new String[this.intents.size()]);
    }

    public String getDisplayType() {
        return displayType;
    }

    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }

    public void setAddable(boolean addable) {
        this.addable = addable;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUserVisible(boolean userVisible) {
        this.userVisible = userVisible;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDataType(PropertyType dataType) {
        this.dataType = dataType;
    }

    public void setPossibleValues(Map<String, String> possibleValues) {
        this.possibleValues = possibleValues;
    }

    public void setBoost(Double boost) {
        this.boost = boost;
    }

    public void setDisplayType(String displayType) {
        this.displayType = displayType;
    }

    public void setPropertyGroup(String propertyGroup) {
        this.propertyGroup = propertyGroup;
    }

    public void setValidationFormula(String validationFormula) {
        this.validationFormula = validationFormula;
    }

    public void setDisplayFormula(String displayFormula) {
        this.displayFormula = displayFormula;
    }

    public void setDependentPropertyIris(ImmutableList<String> dependentPropertyIris) {
        this.dependentPropertyIris = dependentPropertyIris == null ? ImmutableList.<String>of() : dependentPropertyIris;
    }

    @Override
    public void setProperty(String name, Object value, Authorizations authorizations) {
        if (OntologyProperties.DISPLAY_TYPE.getPropertyName().equals(name)) {
            this.displayType = (String) value;
        } else if (OntologyProperties.DISPLAY_FORMULA.getPropertyName().equals(name)) {
            this.displayFormula = (String) value;
        } else if (OntologyProperties.DISPLAY_NAME.getPropertyName().equals(name)) {
            this.displayName = (String) value;
        } else if (OntologyProperties.EDGE_LABEL_DEPENDENT_PROPERTY.equals(name)) {
            this.dependentPropertyIris = ImmutableList.copyOf(JSONUtil.toStringList(JSONUtil.parseArray((String) value)));
        } else if (OntologyProperties.SEARCHABLE.getPropertyName().equals(name)) {
            if (value instanceof Boolean) {
                this.searchable = (Boolean) value;
            } else {
                this.searchable = Boolean.parseBoolean((String) value);
            }
        } else if (OntologyProperties.ADDABLE.getPropertyName().equals(name)) {
            if (value instanceof Boolean) {
                this.addable = (Boolean) value;
            } else {
                this.addable = Boolean.parseBoolean((String) value);
            }
        } else if (OntologyProperties.USER_VISIBLE.getPropertyName().equals(name)) {
            if (value instanceof Boolean) {
                this.userVisible = (Boolean) value;
            } else {
                this.userVisible = Boolean.parseBoolean((String) value);
            }
        }
    }

    public void addIntent(String intent) {
        this.intents.add(intent);
    }
}
