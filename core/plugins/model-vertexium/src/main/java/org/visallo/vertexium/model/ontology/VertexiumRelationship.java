package org.visallo.vertexium.model.ontology;

import org.vertexium.Authorizations;
import org.visallo.core.model.ontology.OntologyProperties;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.Relationship;
import org.vertexium.Vertex;
import org.vertexium.util.IterableUtils;

import java.util.List;

public class VertexiumRelationship extends Relationship {
    private final Vertex vertex;
    private final List<String> inverseOfIRIs;

    public VertexiumRelationship(Vertex vertex, List<String> domainConceptIRIs, List<String> rangeConceptIRIs, List<String> inverseOfIRIs) {
        super(domainConceptIRIs, rangeConceptIRIs);
        this.vertex = vertex;
        this.inverseOfIRIs = inverseOfIRIs;
    }

    @Override
    public String[] getIntents() {
        return IterableUtils.toArray(OntologyProperties.INTENT.getPropertyValues(vertex), String.class);
    }

    @Override
    public void setProperty(String name, Object value, Authorizations authorizations) {
        getVertex().setProperty(name, value, OntologyRepository.VISIBILITY.getVisibility(), authorizations);
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

    public Vertex getVertex() {
        return vertex;
    }
}
