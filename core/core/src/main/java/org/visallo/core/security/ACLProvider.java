package org.visallo.core.security;

import org.vertexium.Edge;
import org.vertexium.Element;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.util.IterableUtils;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.*;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public void checkCanAddOrUpdateProperty(Element element, String propertyKey, String propertyName, User user)
            throws VisalloAccessDeniedException {
        boolean isUpdate = element.getProperty(propertyKey, propertyName) != null;
        boolean canAddOrUpdate = isUpdate
                ? canUpdateProperty(element, propertyKey, propertyName, user)
                : canAddProperty(element, propertyKey, propertyName, user);

        if (canAddOrUpdate && isUpdate && isComment(propertyName)) {
            canAddOrUpdate = isAuthor(element, propertyKey, propertyName, user);
        }

        if (!canAddOrUpdate) {
            throw new VisalloAccessDeniedException(
                    propertyName + " cannot be added or updated due to ACL restriction", user, element.getId());
        }
    }

    public void checkCanDeleteProperty(Element element, String propertyKey, String propertyName, User user)
            throws VisalloAccessDeniedException {
        boolean canDelete = canDeleteProperty(element, propertyKey, propertyName, user);

        if (canDelete && isComment(propertyName)) {
            canDelete = isAuthor(element, propertyKey, propertyName, user);
        }

        if (!canDelete) {
            throw new VisalloAccessDeniedException(
                    propertyName + " cannot be deleted due to ACL restriction", user, element.getId());
        }
    }

    public ClientApiElementAcl elementACL(Element element, User user, OntologyRepository ontologyRepository) {
        ClientApiElementAcl elementAcl = new ClientApiElementAcl();
        elementAcl.setAddable(true);
        elementAcl.setUpdateable(canUpdateElement(element, user));
        elementAcl.setDeleteable(canDeleteElement(element, user));

        List<ClientApiPropertyAcl> propertyAcls = elementAcl.getPropertyAcls();
        if (element instanceof Vertex) {
            String iri = VisalloProperties.CONCEPT_TYPE.getPropertyValue(element);
            while (iri != null) {
                Concept concept = ontologyRepository.getConceptByIRI(iri);
                populatePropertyAcls(concept, element, user, propertyAcls);
                iri = concept.getParentConceptIRI();
            }
        } else if (element instanceof Edge) {
            Relationship relationship = ontologyRepository.getRelationshipByIRI(((Edge) element).getLabel());
            populatePropertyAcls(relationship, element, user, propertyAcls);
        } else {
            throw new VisalloException("unsupported Element class " + element.getClass().getName());
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

    public Set<String> getAllPrivileges() {
        return Privilege.getAllBuiltIn();
    }

    protected boolean isComment(String propertyName) {
        return VisalloProperties.COMMENT.isSameName(propertyName);
    }

    protected boolean isAuthor(Element element, String propertyKey, String propertyName, User user) {
        Property property = element.getProperty(propertyKey, propertyName);
        String authorUserId = VisalloProperties.MODIFIED_BY_METADATA.getMetadataValue(property.getMetadata());
        return user.getUserId().equals(authorUserId);
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

    private void populatePropertyAcls(HasOntologyProperties hasOntologyProperties, Element element, User user,
                                      List<ClientApiPropertyAcl> propertyAcls) {
        Collection<OntologyProperty> ontologyProperties = hasOntologyProperties.getProperties();
        Set<String> addedPropertyNames = new HashSet<>();
        for (OntologyProperty ontologyProperty : ontologyProperties) {
            String name = ontologyProperty.getTitle();
            List<Property> properties = IterableUtils.toList(element.getProperties(name));
            for (Property property : properties) {
                propertyAcls.add(newClientApiPropertyAcl(element, property.getKey(), name, user));
                addedPropertyNames.add(name);
            }
        }

        // for properties that don't exist on the element, use the ontology property definition and omit the key.
        propertyAcls.addAll(ontologyProperties.stream()
                .filter(ontologyProperty -> !addedPropertyNames.contains(ontologyProperty.getTitle()))
                .map(ontologyProperty -> newClientApiPropertyAcl(element, null, ontologyProperty.getTitle(), user))
                .collect(Collectors.toList()));
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
