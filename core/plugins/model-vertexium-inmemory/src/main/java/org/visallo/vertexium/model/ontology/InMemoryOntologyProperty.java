package org.visallo.vertexium.model.ontology;

import com.google.common.collect.ImmutableList;
import org.vertexium.Authorizations;
import org.visallo.core.model.ontology.OntologyProperties;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.web.clientapi.model.PropertyType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class InMemoryOntologyProperty extends OntologyProperty {
    private String title;
    private boolean userVisible;
    private boolean searchable;
    private boolean sortable;
    private boolean addable;
    private String displayName;
    private String propertyGroup;
    private PropertyType dataType;
    private Map<String, String> possibleValues;
    private String displayType;
    private Double boost;
    private String validationFormula;
    private String displayFormula;
    private boolean updateable;
    private boolean deleteable;
    private ImmutableList<String> dependentPropertyIris = ImmutableList.of();
    private List<String> intents = new ArrayList<>();
    private List<String> textIndexHints = new ArrayList<>();

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getId() {
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
    public boolean getSortable() {
        return sortable;
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

    @Override
    public boolean getDeleteable() { return deleteable; }

    @Override
    public boolean getUpdateable() { return updateable; }

    public String[] getIntents() {
        return this.intents.toArray(new String[this.intents.size()]);
    }

    @Override
    public String[] getTextIndexHints() {
        return this.textIndexHints.toArray(new String[this.textIndexHints.size()]);
    }

    @Override
    public void addTextIndexHints(String textIndexHints, Authorizations authorizations) {
        addTextIndexHints(textIndexHints);
    }

    public void addTextIndexHints (String textIndexHints) {
        this.textIndexHints.add(textIndexHints);
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

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
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

    public void setDependentPropertyIris(Collection<String> dependentPropertyIris) {
        this.dependentPropertyIris = dependentPropertyIris == null ? ImmutableList.<String>of() : ImmutableList.copyOf(dependentPropertyIris);
    }

    public void setUpdateable(boolean updateable) { this.updateable = updateable; }

    public void setDeleteable(boolean deleteable) { this.deleteable = deleteable; }

    @Override
    public void setProperty(String name, Object value, Authorizations authorizations) {
        if (OntologyProperties.DISPLAY_TYPE.getPropertyName().equals(name)) {
            this.displayType = (String) value;
        } else if (OntologyProperties.DISPLAY_FORMULA.getPropertyName().equals(name)) {
            this.displayFormula = (String) value;
        } else if (OntologyProperties.DISPLAY_NAME.getPropertyName().equals(name)) {
            this.displayName = (String) value;
        } else if (OntologyProperties.PROPERTY_GROUP.getPropertyName().equals(name)) {
            this.propertyGroup = (String) value;
        } else if (OntologyProperties.SEARCHABLE.getPropertyName().equals(name)) {
            if (value instanceof Boolean) {
                this.searchable = (Boolean) value;
            } else {
                this.searchable = Boolean.parseBoolean((String) value);
            }
        } else if (OntologyProperties.SORTABLE.getPropertyName().equals(name)) {
            if (value instanceof Boolean) {
                this.sortable = (Boolean) value;
            } else {
                this.sortable = Boolean.parseBoolean((String) value);
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
        } else if (OntologyProperties.DELETEABLE.getPropertyName().equals(name)) {
            if (value instanceof Boolean) {
                this.deleteable = (Boolean) value;
            } else {
                this.deleteable = Boolean.parseBoolean((String) value);
            }
        } else if (OntologyProperties.UPDATEABLE.getPropertyName().equals(name)) {
            if (value instanceof Boolean) {
                this.updateable = (Boolean) value;
            } else {
                this.updateable = Boolean.parseBoolean((String) value);
            }
        }
    }

    public void addIntent(String intent) {
        this.intents.add(intent);
    }

    @Override
    public void addIntent(String intent, Authorizations authorizations) {
        this.intents.add(intent);
    }

    @Override
    public void removeIntent(String intent, Authorizations authorizations) {
        this.intents.remove(intent);
    }
}
