package org.visallo.core.security;

import org.vertexium.Element;
import org.vertexium.Property;
import org.vertexium.util.IterableUtils;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.*;

import java.util.Collection;
import java.util.List;

public abstract class ACLProvider {
    public abstract boolean canDeleteElement(Element element, User user);

    public abstract boolean canDeleteProperty(Element element, String propertyKey, String propertyName, User user);

    public abstract boolean canUpdateElement(Element element, User user);

    public abstract boolean canUpdateProperty(Element element, String propertyKey, String propertyName, User user);

    public abstract boolean canAddProperty(Element element, String propertyKey, String propertyName, User user);

    public abstract boolean canDeleteElement(ClientApiElement element, User user);

    public abstract boolean canDeleteProperty(ClientApiElement element, ClientApiProperty p, User user);

    public abstract boolean canUpdateElement(ClientApiElement element, User user);

    public abstract boolean canUpdateProperty(ClientApiElement element, ClientApiProperty p, User user);

    public abstract boolean canAddProperty(ClientApiElement element, ClientApiProperty p, User user);

    public boolean canAddOrUpdateProperty(Element element, String propertyKey, String propertyName, User user) {
        return canUpdateElement(element, user) &&
                (element.getProperty(propertyKey, propertyName) != null
                        ? canUpdateProperty(element, propertyKey, propertyName, user)
                        : canAddProperty(element, propertyKey, propertyName, user));
    }

    public ClientApiElementAcl elementACL(Element element, User user, OntologyRepository ontologyRepository) {
        ClientApiElementAcl elementAcl = new ClientApiElementAcl();
        elementAcl.setAddable(true);
        elementAcl.setUpdateable(canUpdateElement(element, user));
        elementAcl.setDeleteable(canDeleteElement(element, user));

        List<ClientApiPropertyAcl> propertyAcls = elementAcl.getPropertyAcls();
        String conceptIri = VisalloProperties.CONCEPT_TYPE.getPropertyValue(element);
        while (conceptIri != null) {
            Concept concept = ontologyRepository.getConceptByIRI(conceptIri);
            populatePropertyAcls(concept, element, user, propertyAcls);
            conceptIri = concept.getParentConceptIRI();
        }
        return elementAcl;
    }

    public ClientApiObject appendACL(ClientApiObject clientApiObject, User user) {
        if (clientApiObject instanceof ClientApiElement) {
            ClientApiElement apiElement = (ClientApiElement) clientApiObject;
            appendACL(apiElement, user);
        } else if (clientApiObject instanceof ClientApiWorkspaceVertices) {
            appendACL(((ClientApiWorkspaceVertices) clientApiObject).getVertices(), user);
        } else if (clientApiObject instanceof ClientApiVertexMultipleResponse) {
            appendACL(((ClientApiVertexMultipleResponse) clientApiObject).getVertices(), user);
        } else if (clientApiObject instanceof ClientApiEdgeMultipleResponse) {
            appendACL(((ClientApiEdgeMultipleResponse) clientApiObject).getEdges(), user);
        } else if (clientApiObject instanceof ClientApiElementSearchResponse) {
            appendACL(((ClientApiElementSearchResponse) clientApiObject).getElements(), user);
        } else if (clientApiObject instanceof ClientApiEdgeSearchResponse) {
            appendACL(((ClientApiEdgeSearchResponse) clientApiObject).getResults(), user);
        } else if (clientApiObject instanceof ClientApiVertexEdges) {
            ClientApiVertexEdges vertexEdges = (ClientApiVertexEdges) clientApiObject;
            appendACL(vertexEdges, user);
        }

        return clientApiObject;
    }

    public void appendACL(Collection<? extends ClientApiObject> clientApiObject, User user) {
        for (ClientApiObject apiObject : clientApiObject) {
            appendACL(apiObject, user);
        }
    }

    protected void appendACL(ClientApiElement apiElement, User user) {
        for (ClientApiProperty property : apiElement.getProperties()) {
            property.setUpdateable(canUpdateProperty(apiElement, property, user));
            property.setDeleteable(canDeleteProperty(apiElement, property, user));
            property.setAddable(canAddProperty(apiElement, property, user));
        }
        apiElement.setUpdateable(canUpdateElement(apiElement, user));
        apiElement.setDeleteable(canDeleteElement(apiElement, user));

        if (apiElement instanceof ClientApiEdgeWithVertexData) {
            appendACL(((ClientApiEdgeWithVertexData) apiElement).getSource(), user);
            appendACL(((ClientApiEdgeWithVertexData) apiElement).getTarget(), user);
        }
    }

    protected void appendACL(ClientApiVertexEdges edges, User user) {
        for (ClientApiVertexEdges.Edge vertexEdge : edges.getRelationships()) {
            appendACL(vertexEdge.getRelationship(), user);
            appendACL(vertexEdge.getVertex(), user);
        }
    }

    private void populatePropertyAcls(Concept concept, Element element, User user, List<ClientApiPropertyAcl> propertyAcls) {
        for (OntologyProperty ontologyProperty : concept.getProperties()) {
            String name = ontologyProperty.getTitle();
            List<Property> properties = IterableUtils.toList(element.getProperties(name));
            if (properties.isEmpty()) {
                ClientApiPropertyAcl propertyAcl = newClientApiPropertyAcl(element, null, name, user);
                propertyAcls.add(propertyAcl);
            } else {
                for (Property property : properties) {
                    String key = property.getKey();
                    ClientApiPropertyAcl propertyAcl = newClientApiPropertyAcl(element, key, name, user);
                    propertyAcls.add(propertyAcl);
                }
            }
        }
    }

    private ClientApiPropertyAcl newClientApiPropertyAcl(Element element, String key, String name, User user) {
        ClientApiPropertyAcl propertyAcl = new ClientApiPropertyAcl();
        propertyAcl.setKey(key);
        propertyAcl.setName(name);
        propertyAcl.setAddable(canAddProperty(element, key, name, user));
        propertyAcl.setUpdateable(canUpdateProperty(element, key, name, user));
        propertyAcl.setDeleteable(canDeleteProperty(element, key, name, user));
        return propertyAcl;
    }
}
