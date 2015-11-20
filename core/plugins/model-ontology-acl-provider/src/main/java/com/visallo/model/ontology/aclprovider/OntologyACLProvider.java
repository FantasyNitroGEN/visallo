package com.visallo.model.ontology.aclprovider;

import com.google.inject.Inject;
import org.vertexium.Edge;
import org.vertexium.Element;
import org.vertexium.Vertex;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.security.ACLProvider;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiEdge;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiProperty;
import org.visallo.web.clientapi.model.ClientApiVertex;

import static com.google.common.base.Preconditions.checkNotNull;

public class OntologyACLProvider extends ACLProvider {
    private OntologyRepository ontologyRepository;

    @Inject
    public OntologyACLProvider(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public boolean canDeleteElement(Element e, final User user) {
        if (e instanceof Edge) {
            return getOntologyRelationshipFromElement(e).getDeleteable();
        } else if (e instanceof Vertex) {
            return getOntologyConceptFromElement(e).getDeleteable();
        }
        throw new VisalloException("Unexpected element type: " + e.getClass().getName());
    }

    @Override
    public boolean canDeleteProperty(Element e, String propertyKey, String propertyName, final User user) {
        boolean isComment = VisalloProperties.COMMENT.getPropertyName().equals(propertyName);
        return isComment || (canUpdateElement(e, user) && getDeleteableFromOntologyProperty(propertyName));
    }

    @Override
    public boolean canUpdateElement(Element e, final User user) {
        if (e instanceof Edge) {
            return getOntologyRelationshipFromElement(e).getUpdateable();
        } else if (e instanceof Vertex) {
            return getOntologyConceptFromElement(e).getUpdateable();
        }
        throw new VisalloException("Unexpected element type: " + e.getClass().getName());
    }

    @Override
    public boolean canUpdateProperty(Element e, String propertyKey, String propertyName, final User user) {
        boolean isComment = VisalloProperties.COMMENT.getPropertyName().equals(propertyName);
        return isComment || (canUpdateElement(e, user) && getUpdateableFromOntologyProperty(propertyName));
    }

    @Override
    public boolean canDeleteElement(ClientApiElement e) {
        if (e instanceof ClientApiVertex) {
            Concept concept = getOntologyConceptFromElement(((ClientApiVertex) e).getConceptType());
            return concept.getDeleteable();
        } else if (e instanceof ClientApiEdge) {
            Relationship relationship = getOntologyRelationshipFromElement(((ClientApiEdge) e).getLabel());
            return relationship.getDeleteable();
        }

        throw new VisalloException("Unexpected client element type: " + e.getClass().getName());
    }

    @Override
    public boolean canDeleteProperty(ClientApiElement e, ClientApiProperty p) {
        boolean isComment = VisalloProperties.COMMENT.getPropertyName().equals(p.getName());
        return isComment || (canUpdateElement(e) && getDeleteableFromOntologyProperty(p.getName()));
    }

    @Override
    public boolean canUpdateElement(ClientApiElement e) {
        if (e instanceof ClientApiVertex) {
            Concept concept = getOntologyConceptFromElement(((ClientApiVertex) e).getConceptType());
            return concept.getUpdateable();
        } else if (e instanceof ClientApiEdge) {
            Relationship relationship = getOntologyRelationshipFromElement(((ClientApiEdge) e).getLabel());
            return relationship.getUpdateable();
        }

        throw new VisalloException("Unexpected client element type: " + e.getClass().getName());
    }

    @Override
    public boolean canUpdateProperty(ClientApiElement e, ClientApiProperty p) {
        boolean isComment = VisalloProperties.COMMENT.getPropertyName().equals(p.getName());
        return isComment || (canUpdateElement(e) && getUpdateableFromOntologyProperty(p.getName()));
    }

    private Relationship getOntologyRelationshipFromElement(Element e) {
        String label = ((Edge) e).getLabel();
        return getOntologyRelationshipFromElement(label);
    }

    private Relationship getOntologyRelationshipFromElement(String edgeLabel) {
        checkNotNull(edgeLabel, "Edge label cannot be null");
        Relationship relationship = ontologyRepository.getRelationshipByIRI(edgeLabel);
        checkNotNull(relationship, edgeLabel + " does not exist in ontology");
        return relationship;
    }

    private Concept getOntologyConceptFromElement(Element e) {
        Vertex vertex = (Vertex) e;
        String iri = VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex, null);
        return getOntologyConceptFromElement(iri);
    }

    private Concept getOntologyConceptFromElement(String conceptType) {
        checkNotNull(conceptType, "Concept type cannot be null");
        Concept concept = ontologyRepository.getConceptByIRI(conceptType);
        checkNotNull(concept, "Concept cannot be null");
        return concept;
    }

    private boolean getDeleteableFromOntologyProperty(String propertyIri) {
        OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyIri);
        return property == null || property.getDeleteable();
    }

    private boolean getUpdateableFromOntologyProperty(String propertyIri) {
        OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyIri);
        return property == null || property.getUpdateable();
    }
}
