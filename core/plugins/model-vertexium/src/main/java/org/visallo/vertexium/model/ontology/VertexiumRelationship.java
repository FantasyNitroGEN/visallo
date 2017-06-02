package org.visallo.vertexium.model.ontology;

import org.vertexium.Authorizations;
import org.vertexium.Vertex;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.util.IterableUtils;
import org.visallo.core.model.ontology.OntologyProperties;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.Relationship;

import java.util.Collection;
import java.util.List;

public class VertexiumRelationship extends Relationship {
    private final Vertex vertex;
    private final List<String> inverseOfIRIs;

    public VertexiumRelationship(
            String parentIRI,
            Vertex vertex,
            List<String> domainConceptIRIs,
            List<String> rangeConceptIRIs,
            List<String> inverseOfIRIs,
            Collection<OntologyProperty> properties
    ) {
        super(parentIRI, domainConceptIRIs, rangeConceptIRIs, properties);
        this.vertex = vertex;
        this.inverseOfIRIs = inverseOfIRIs;
    }

    @Override
    public String getId() {
        return vertex.getId();
    }

    @Override
    public String[] getIntents() {
        return IterableUtils.toArray(OntologyProperties.INTENT.getPropertyValues(vertex), String.class);
    }

    @Override
    public void addIntent(String intent, Authorizations authorizations) {
        OntologyProperties.INTENT.addPropertyValue(vertex, intent, intent, OntologyRepository.VISIBILITY.getVisibility(), authorizations);
        getVertex().getGraph().flush();
    }

    @Override
    public void removeIntent(String intent, Authorizations authorizations) {
        OntologyProperties.INTENT.removeProperty(vertex, intent, authorizations);
        getVertex().getGraph().flush();
    }

    @Override
    public void setProperty(String name, Object value, Authorizations authorizations) {
        getVertex().setProperty(name, value, OntologyRepository.VISIBILITY.getVisibility(), authorizations);
        getVertex().getGraph().flush();
    }

    @Override
    public void removeProperty(String name, Authorizations authorizations) {
        getVertex().softDeleteProperty(ElementMutation.DEFAULT_KEY, name, authorizations);
        getVertex().getGraph().flush();
    }

    @Override
    public String getTitleFormula() {
        return OntologyProperties.TITLE_FORMULA.getPropertyValue(vertex);
    }

    @Override
    public String getSubtitleFormula() {
        return OntologyProperties.SUBTITLE_FORMULA.getPropertyValue(vertex);
    }

    @Override
    public String getTimeFormula() {
        return OntologyProperties.TIME_FORMULA.getPropertyValue(vertex);
    }

    public String getIRI() {
        return OntologyProperties.ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    public String getDisplayName() {
        return OntologyProperties.DISPLAY_NAME.getPropertyValue(vertex);
    }

    @Override
    public Iterable<String> getInverseOfIRIs() {
        return inverseOfIRIs;
    }

    @Override
    public boolean getUserVisible() {
        return OntologyProperties.USER_VISIBLE.getPropertyValue(vertex, true);
    }

    @Override
    public boolean getDeleteable() {
        return OntologyProperties.DELETEABLE.getPropertyValue(vertex, true);
    }

    @Override
    public boolean getUpdateable() {
        return OntologyProperties.UPDATEABLE.getPropertyValue(vertex, true);
    }

    public Vertex getVertex() {
        return vertex;
    }
}
