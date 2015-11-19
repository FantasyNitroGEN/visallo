package org.visallo.vertexium.model.ontology;

import com.google.common.collect.ImmutableList;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Vertex;
import org.vertexium.util.IterableUtils;
import org.visallo.core.model.ontology.OntologyProperties;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.JSONUtil;
import org.visallo.web.clientapi.model.PropertyType;

import java.util.Collection;
import java.util.Map;

public class VertexiumOntologyProperty extends OntologyProperty {
    private final Vertex vertex;
    private ImmutableList<String> dependentPropertyIris;

    public VertexiumOntologyProperty(Vertex vertex, ImmutableList<String> dependentPropertyIris) {
        this.vertex = vertex;
        this.dependentPropertyIris = dependentPropertyIris;
    }

    @Override
    public void setProperty(String name, Object value, Authorizations authorizations) {
        getVertex().setProperty(name, value, OntologyRepository.VISIBILITY.getVisibility(), authorizations);
    }

    public String getTitle() {
        return OntologyProperties.ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    public String getDisplayName() {
        return OntologyProperties.DISPLAY_NAME.getPropertyValue(vertex);
    }

    public String getPropertyGroup() {
        return OntologyProperties.PROPERTY_GROUP.getPropertyValue(vertex);
    }

    @Override
    public String getValidationFormula() {
        return OntologyProperties.VALIDATION_FORMULA.getPropertyValue(vertex);
    }

    @Override
    public String getDisplayFormula() {
        return OntologyProperties.DISPLAY_FORMULA.getPropertyValue(vertex);
    }

    @Override
    public ImmutableList<String> getDependentPropertyIris() {
        return this.dependentPropertyIris;
    }

    public String[] getIntents() {
        return IterableUtils.toArray(OntologyProperties.INTENT.getPropertyValues(vertex), String.class);
    }

    @Override
    public void addIntent(String intent, Authorizations authorizations) {
        OntologyProperties.INTENT.addPropertyValue(vertex, intent, intent, OntologyRepository.VISIBILITY.getVisibility(), authorizations);
    }

    @Override
    public void removeIntent(String intent, Authorizations authorizations) {
        OntologyProperties.INTENT.removeProperty(vertex, intent, authorizations);
    }

    public boolean getUserVisible() {
        Boolean b = OntologyProperties.USER_VISIBLE.getPropertyValue(vertex);
        if (b == null) {
            return true;
        }
        return b;
    }

    public boolean getSearchable() {
        Boolean b = OntologyProperties.SEARCHABLE.getPropertyValue(vertex);
        if (b == null) {
            return true;
        }
        return b;
    }

    public boolean getSortable() {
        Boolean b = OntologyProperties.SORTABLE.getPropertyValue(vertex);
        if (b == null) {
            return true;
        }
        return b;
    }

    public boolean getUpdateable() {
        Boolean b = OntologyProperties.UPDATEABLE.getPropertyValue(vertex);
        if (b == null) {
            return true;
        }
        return b;
    }

    public boolean getDeleteable() {
        Boolean b = OntologyProperties.DELETEABLE.getPropertyValue(vertex);
        if (b == null) {
            return true;
        }
        return b;
    }

    @Override
    public boolean getAddable() {
        Boolean b = OntologyProperties.ADDABLE.getPropertyValue(vertex);
        if (b == null) {
            return true;
        }
        return b;
    }

    public PropertyType getDataType() {
        return PropertyType.convert(OntologyProperties.DATA_TYPE.getPropertyValue(vertex));
    }

    public String getDisplayType() {
        return OntologyProperties.DISPLAY_TYPE.getPropertyValue(vertex);
    }

    @Override
    public Double getBoost() {
        return OntologyProperties.BOOST.getPropertyValue(vertex);
    }

    public Map<String, String> getPossibleValues() {
        JSONObject propertyValue = OntologyProperties.POSSIBLE_VALUES.getPropertyValue(vertex);
        if (propertyValue == null) {
            return null;
        }
        return JSONUtil.toMap(propertyValue);
    }

    public Vertex getVertex() {
        return this.vertex;
    }

    public void setDependentProperties(Collection<String> newDependentPropertyIris) {
        this.dependentPropertyIris = ImmutableList.copyOf(newDependentPropertyIris);
    }
}
