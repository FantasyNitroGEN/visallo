package org.visallo.core.security;

import com.google.inject.Inject;
import org.vertexium.*;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.*;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.visallo.core.model.properties.VisalloProperties.CONCEPT_TYPE_THING;

public abstract class ACLProvider {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ACLProvider.class);
    protected final Graph graph;
    protected final UserRepository userRepository;
    protected final OntologyRepository ontologyRepository;
    private final PrivilegeRepository privilegeRepository;

    @Inject
    protected ACLProvider(
            Graph graph,
            UserRepository userRepository,
            OntologyRepository ontologyRepository,
            PrivilegeRepository privilegeRepository
    ) {
        this.graph = graph;
        this.userRepository = userRepository;
        this.ontologyRepository = ontologyRepository;
        this.privilegeRepository = privilegeRepository;
    }

    public boolean canDeleteElement(Element element, User user) {
        OntologyElement ontologyElement = getOntologyElement(element);
        return canDeleteElement(element, ontologyElement, user);
    }

    protected abstract boolean canDeleteElement(Element element, OntologyElement ontologyElement, User user);

    public boolean canDeleteElement(ClientApiElement clientApiElement, User user) {
        OntologyElement ontologyElement = getOntologyElement(clientApiElement);
        return canDeleteElement(clientApiElement, ontologyElement, user);
    }

    protected abstract boolean canDeleteElement(ClientApiElement clientApiElement, OntologyElement ontologyElement, User user);

    public boolean canDeleteProperty(Element element, String propertyKey, String propertyName, User user) {
        OntologyElement ontologyElement = getOntologyElement(element);
        return canDeleteProperty(element, ontologyElement, propertyKey, propertyName, user);
    }

    protected abstract boolean canDeleteProperty(Element element, OntologyElement ontologyElement, String propertyKey, String propertyName, User user);

    public boolean canDeleteProperty(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user) {
        OntologyElement ontologyElement = getOntologyElement(clientApiElement);
        return canDeleteProperty(clientApiElement, ontologyElement, propertyKey, propertyName, user);
    }

    protected abstract boolean canDeleteProperty(ClientApiElement clientApiElement, OntologyElement ontologyElement, String propertyKey, String propertyName, User user);

    public boolean canUpdateElement(Element element, User user) {
        OntologyElement ontologyElement = getOntologyElement(element);
        return canUpdateElement(element, ontologyElement, user);
    }

    protected abstract boolean canUpdateElement(Element element, OntologyElement ontologyElement, User user);

    public boolean canUpdateElement(ClientApiElement clientApiElement, User user) {
        OntologyElement ontologyElement = getOntologyElement(clientApiElement);
        return canUpdateElement(clientApiElement, ontologyElement, user);
    }

    protected abstract boolean canUpdateElement(ClientApiElement clientApiElement, OntologyElement ontologyElement, User user);

    public boolean canUpdateProperty(Element element, String propertyKey, String propertyName, User user) {
        OntologyElement ontologyElement = getOntologyElement(element);
        return canUpdateProperty(element, ontologyElement, propertyKey, propertyName, user);
    }

    protected abstract boolean canUpdateProperty(Element element, OntologyElement ontologyElement, String propertyKey, String propertyName, User user);

    public boolean canUpdateProperty(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user) {
        OntologyElement ontologyElement = getOntologyElement(clientApiElement);
        return canUpdateProperty(clientApiElement, ontologyElement, propertyKey, propertyName, user);
    }

    protected abstract boolean canUpdateProperty(ClientApiElement clientApiElement, OntologyElement ontologyElement, String propertyKey, String propertyName, User user);

    public boolean canAddProperty(Element element, String propertyKey, String propertyName, User user) {
        OntologyElement ontologyElement = getOntologyElement(element);
        return canAddProperty(element, ontologyElement, propertyKey, propertyName, user);
    }

    protected abstract boolean canAddProperty(Element element, OntologyElement ontologyElement, String propertyKey, String propertyName, User user);

    public boolean canAddProperty(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user) {
        OntologyElement ontologyElement = getOntologyElement(clientApiElement);
        return canAddProperty(clientApiElement, ontologyElement, propertyKey, propertyName, user);
    }

    protected abstract boolean canAddProperty(ClientApiElement clientApiElement, OntologyElement ontologyElement, String propertyKey, String propertyName, User user);

    public final void checkCanAddOrUpdateProperty(Element element, String propertyKey, String propertyName, User user) {
        OntologyElement ontologyElement = getOntologyElement(element);
        checkCanAddOrUpdateProperty(element, ontologyElement, propertyKey, propertyName, user);
    }

    private void checkCanAddOrUpdateProperty(
            Element element,
            OntologyElement ontologyElement,
            String propertyKey,
            String propertyName,
            User user
    )
            throws VisalloAccessDeniedException {
        boolean isUpdate = element.getProperty(propertyKey, propertyName) != null;
        boolean canAddOrUpdate = isUpdate
                ? internalCanUpdateProperty(element, ontologyElement, propertyKey, propertyName, user)
                : internalCanAddProperty(element, ontologyElement, propertyKey, propertyName, user);

        if (!canAddOrUpdate) {
            throw new VisalloAccessDeniedException(
                    propertyName + " cannot be added or updated due to ACL restriction", user, element.getId());
        }
    }

    public final void checkCanAddOrUpdateProperty(
            ClientApiElement clientApiElement,
            String propertyKey,
            String propertyName,
            User user
    ) {
        OntologyElement ontologyElement = getOntologyElement(clientApiElement);
        checkCanAddOrUpdateProperty(clientApiElement, ontologyElement, propertyKey, propertyName, user);
    }

    public final void checkCanAddOrUpdateProperty(
            ClientApiElement clientApiElement,
            OntologyElement ontologyElement,
            String propertyKey,
            String propertyName,
            User user
    )
            throws VisalloAccessDeniedException {
        boolean isUpdate = clientApiElement.getProperty(propertyKey, propertyName) != null;
        boolean canAddOrUpdate = isUpdate
                ? internalCanUpdateProperty(clientApiElement, ontologyElement, propertyKey, propertyName, user)
                : internalCanAddProperty(clientApiElement, ontologyElement, propertyKey, propertyName, user);

        if (!canAddOrUpdate) {
            throw new VisalloAccessDeniedException(
                    propertyName + " cannot be added or updated due to ACL restriction", user, clientApiElement.getId());
        }
    }

    public final void checkCanDeleteProperty(Element element, String propertyKey, String propertyName, User user) {
        OntologyElement ontologyElement = getOntologyElement(element);
        checkCanDeleteProperty(element, ontologyElement, propertyKey, propertyName, user);
    }

    private void checkCanDeleteProperty(Element element, OntologyElement ontologyElement, String propertyKey, String propertyName, User user)
            throws VisalloAccessDeniedException {
        boolean canDelete = internalCanDeleteProperty(element, ontologyElement, propertyKey, propertyName, user);

        if (!canDelete) {
            throw new VisalloAccessDeniedException(
                    propertyName + " cannot be deleted due to ACL restriction", user, element.getId());
        }
    }

    public final void checkCanDeleteProperty(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user) {
        OntologyElement ontologyElement = getOntologyElement(clientApiElement);
        checkCanDeleteProperty(clientApiElement, ontologyElement, propertyKey, propertyName, user);
    }

    private void checkCanDeleteProperty(ClientApiElement clientApiElement, OntologyElement ontologyElement, String propertyKey, String propertyName, User user)
            throws VisalloAccessDeniedException {
        boolean canDelete = internalCanDeleteProperty(clientApiElement, ontologyElement, propertyKey, propertyName, user);

        if (!canDelete) {
            throw new VisalloAccessDeniedException(
                    propertyName + " cannot be deleted due to ACL restriction", user, clientApiElement.getId());
        }
    }

    public final ClientApiElementAcl elementACL(ClientApiElement clientApiElement, User user) {
        OntologyElement ontologyElement = getOntologyElement(clientApiElement);
        return elementACL(clientApiElement, ontologyElement, user);
    }

    private ClientApiElementAcl elementACL(ClientApiElement clientApiElement, OntologyElement ontologyElement, User user) {
        checkNotNull(clientApiElement, "clientApiElement is required");
        ClientApiElementAcl elementAcl = new ClientApiElementAcl();
        elementAcl.setAddable(true);
        elementAcl.setUpdateable(internalCanUpdateElement(clientApiElement, ontologyElement, user));
        elementAcl.setDeleteable(internalCanDeleteElement(clientApiElement, ontologyElement, user));

        List<ClientApiPropertyAcl> propertyAcls = elementAcl.getPropertyAcls();
        if (clientApiElement instanceof ClientApiVertex) {
            String iri = VisalloProperties.CONCEPT_TYPE.getPropertyValue(clientApiElement);
            while (iri != null) {
                Concept concept = ontologyRepository.getConceptByIRI(iri);
                if (concept == null) {
                    LOGGER.warn("Could not find concept: %s", iri);
                    break;
                }
                populatePropertyAcls(concept, clientApiElement, ontologyElement, user, propertyAcls);
                iri = concept.getParentConceptIRI();
            }
        } else if (clientApiElement instanceof ClientApiEdge) {
            String iri = ((ClientApiEdge) clientApiElement).getLabel();
            while (iri != null) {
                Relationship relationship = ontologyRepository.getRelationshipByIRI(iri);
                if (relationship == null) {
                    LOGGER.warn("Could not find relationship: %s", iri);
                    break;
                }
                populatePropertyAcls(relationship, clientApiElement, ontologyElement, user, propertyAcls);
                iri = relationship.getParentIRI();
            }
        } else {
            throw new VisalloException("unsupported ClientApiElement class " + clientApiElement.getClass().getName());
        }
        return elementAcl;
    }

    public final ClientApiObject appendACL(ClientApiObject clientApiObject, User user) {
        if (clientApiObject instanceof ClientApiElement) {
            appendACL((ClientApiElement) clientApiObject, user);
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
        } else if (clientApiObject instanceof ClientApiElementFindRelatedResponse) {
            appendACL(((ClientApiElementFindRelatedResponse) clientApiObject).getElements(), user);
        }

        return clientApiObject;
    }

    protected final boolean isComment(String propertyName) {
        return VisalloProperties.COMMENT.isSameName(propertyName);
    }

    protected final boolean isAuthor(Element element, String propertyKey, String propertyName, User user) {
        if (element == null) {
            return false;
        }
        Property property = element.getProperty(propertyKey, propertyName);
        if (property != null) {
            String authorUserId = VisalloProperties.MODIFIED_BY_METADATA.getMetadataValue(property.getMetadata());
            return user.getUserId().equals(authorUserId);
        } else {
            return false;
        }
    }

    protected final boolean isAuthor(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user) {
        if (clientApiElement == null) {
            return false;
        }
        ClientApiProperty property = clientApiElement.getProperty(propertyKey, propertyName);
        if (property != null) {
            String authorUserId = VisalloProperties.MODIFIED_BY_METADATA.getMetadataValue(property.getMetadata());
            return user.getUserId().equals(authorUserId);
        } else {
            return false;
        }
    }

    protected final boolean hasPrivilege(User user, String privilege) {
        return privilegeRepository.hasPrivilege(user, privilege);
    }

    private void appendACL(Collection<? extends ClientApiObject> clientApiObject, User user) {
        for (ClientApiObject apiObject : clientApiObject) {
            appendACL(apiObject, user);
        }
    }

    private void appendACL(ClientApiElement clientApiElement, User user) {
        OntologyElement ontologyElement = getOntologyElement(clientApiElement);
        appendACL(clientApiElement, ontologyElement, user);
    }

    private void appendACL(ClientApiElement clientApiElement, OntologyElement ontologyElement, User user) {
        for (ClientApiProperty apiProperty : clientApiElement.getProperties()) {
            String key = apiProperty.getKey();
            String name = apiProperty.getName();
            apiProperty.setUpdateable(internalCanUpdateProperty(clientApiElement, ontologyElement, key, name, user));
            apiProperty.setDeleteable(internalCanDeleteProperty(clientApiElement, ontologyElement, key, name, user));
            apiProperty.setAddable(internalCanAddProperty(clientApiElement, ontologyElement, key, name, user));
        }
        clientApiElement.setUpdateable(internalCanUpdateElement(clientApiElement, ontologyElement, user));
        clientApiElement.setDeleteable(internalCanDeleteElement(clientApiElement, ontologyElement, user));

        clientApiElement.setAcl(elementACL(clientApiElement, ontologyElement, user));

        if (clientApiElement instanceof ClientApiEdgeWithVertexData) {
            appendACL(((ClientApiEdgeWithVertexData) clientApiElement).getSource(), user);
            appendACL(((ClientApiEdgeWithVertexData) clientApiElement).getTarget(), user);
        }
    }

    private void appendACL(ClientApiVertexEdges edges, User user) {
        for (ClientApiVertexEdges.Edge vertexEdge : edges.getRelationships()) {
            appendACL(vertexEdge.getRelationship(), user);
            appendACL(vertexEdge.getVertex(), user);
        }
    }

    private void populatePropertyAcls(
            HasOntologyProperties hasOntologyProperties,
            ClientApiElement clientApiElement,
            OntologyElement ontologyElement,
            User user,
            List<ClientApiPropertyAcl> propertyAcls
    ) {
        Collection<OntologyProperty> ontologyProperties = hasOntologyProperties.getProperties();
        Set<String> addedPropertyNames = new HashSet<>();
        for (OntologyProperty ontologyProperty : ontologyProperties) {
            String name = ontologyProperty.getTitle();
            for (ClientApiProperty property : clientApiElement.getProperties(name)) {
                ClientApiPropertyAcl acl = newClientApiPropertyAcl(clientApiElement, ontologyElement, property.getKey(), name, user);
                ClientApiPropertyAcl defaultAcl = newClientApiPropertyAcl(null, ontologyElement, property.getKey(), name, user);
                if (!acl.equals(defaultAcl)) {
                    propertyAcls.add(acl);
                }
                addedPropertyNames.add(name);
            }
        }

        // for properties that don't exist on the clientApiElement, use the ontology property definition and omit the key.
        propertyAcls.addAll(
                ontologyProperties.stream()
                        .filter(ontologyProperty -> !addedPropertyNames.contains(ontologyProperty.getTitle()))
                        .map(ontologyProperty -> {
                            ClientApiPropertyAcl acl = newClientApiPropertyAcl(clientApiElement, ontologyElement, null, ontologyProperty.getTitle(), user);
                            ClientApiPropertyAcl defaultAcl = newClientApiPropertyAcl(null, ontologyElement, null, ontologyProperty.getTitle(), user);
                            return acl.equals(defaultAcl) ? null : acl;
                        })
                        .filter(acl -> acl != null)
                        .collect(Collectors.toList())
        );
    }

    private ClientApiPropertyAcl newClientApiPropertyAcl(
            ClientApiElement clientApiElement,
            OntologyElement ontologyElement,
            String key,
            String name,
            User user
    ) {
        ClientApiPropertyAcl propertyAcl = new ClientApiPropertyAcl();
        propertyAcl.setKey(key);
        propertyAcl.setName(name);
        propertyAcl.setAddable(internalCanAddProperty(clientApiElement, ontologyElement, key, name, user));
        propertyAcl.setUpdateable(internalCanUpdateProperty(clientApiElement, ontologyElement, key, name, user));
        propertyAcl.setDeleteable(internalCanDeleteProperty(clientApiElement, ontologyElement, key, name, user));
        return propertyAcl;
    }

    private boolean internalCanDeleteElement(ClientApiElement clientApiElement, OntologyElement ontologyElement, User user) {
        return hasPrivilege(user, Privilege.EDIT) && canDeleteElement(clientApiElement, ontologyElement, user);
    }

    private boolean internalCanUpdateElement(ClientApiElement clientApiElement, OntologyElement ontologyElement, User user) {
        return hasPrivilege(user, Privilege.EDIT) && canUpdateElement(clientApiElement, ontologyElement, user);
    }

    private boolean internalCanDeleteProperty(Element element, OntologyElement ontologyElement, String propertyKey, String propertyName, User user) {
        boolean canDelete = hasEditOrCommentPrivilege(propertyName, user)
                && canDeleteProperty(element, ontologyElement, propertyKey, propertyName, user);
        if (canDelete && isComment(propertyName)) {
            canDelete = hasPrivilege(user, Privilege.COMMENT_DELETE_ANY) ||
                    (hasPrivilege(user, Privilege.COMMENT) && isAuthor(element, propertyKey, propertyName, user));
        }
        return canDelete;
    }

    private boolean internalCanDeleteProperty(ClientApiElement clientApiElement, OntologyElement ontologyElement, String propertyKey, String propertyName, User user) {
        boolean canDelete = hasEditOrCommentPrivilege(propertyName, user)
                && canDeleteProperty(clientApiElement, ontologyElement, propertyKey, propertyName, user);
        if (canDelete && isComment(propertyName)) {
            canDelete = hasPrivilege(user, Privilege.COMMENT_DELETE_ANY) ||
                    (hasPrivilege(user, Privilege.COMMENT) && isAuthor(clientApiElement, propertyKey, propertyName, user));
        }
        return canDelete;
    }

    private boolean internalCanUpdateProperty(Element element, OntologyElement ontologyElement, String propertyKey, String propertyName, User user) {
        boolean canUpdate = hasEditOrCommentPrivilege(propertyName, user)
                && canUpdateProperty(element, ontologyElement, propertyKey, propertyName, user);
        if (canUpdate && isComment(propertyName)) {
            canUpdate = hasPrivilege(user, Privilege.COMMENT_EDIT_ANY) ||
                    (hasPrivilege(user, Privilege.COMMENT) && isAuthor(element, propertyKey, propertyName, user));
        }
        return canUpdate;
    }

    private boolean internalCanUpdateProperty(ClientApiElement clientApiElement, OntologyElement ontologyElement, String propertyKey, String propertyName, User user) {
        boolean canUpdate = hasEditOrCommentPrivilege(propertyName, user)
                && canUpdateProperty(clientApiElement, ontologyElement, propertyKey, propertyName, user);
        if (canUpdate && isComment(propertyName)) {
            canUpdate = hasPrivilege(user, Privilege.COMMENT_EDIT_ANY) ||
                    (hasPrivilege(user, Privilege.COMMENT) && isAuthor(clientApiElement, propertyKey, propertyName, user));
        }
        return canUpdate;
    }

    private boolean internalCanAddProperty(Element element, OntologyElement ontologyElement, String propertyKey, String propertyName, User user) {
        boolean canAdd = hasEditOrCommentPrivilege(propertyName, user)
                && canAddProperty(element, ontologyElement, propertyKey, propertyName, user);
        if (canAdd && isComment(propertyName)) {
            canAdd = hasPrivilege(user, Privilege.COMMENT);
        }
        return canAdd;
    }

    private boolean internalCanAddProperty(ClientApiElement clientApiElement, OntologyElement ontologyElement, String propertyKey, String propertyName, User user) {
        boolean canAdd = hasEditOrCommentPrivilege(propertyName, user)
                && canAddProperty(clientApiElement, ontologyElement, propertyKey, propertyName, user);
        if (canAdd && isComment(propertyName)) {
            canAdd = hasPrivilege(user, Privilege.COMMENT);
        }
        return canAdd;
    }

    private boolean hasEditOrCommentPrivilege(String propertyName, User user) {
        return hasPrivilege(user, Privilege.EDIT) || (isComment(propertyName) && hasPrivilege(user, Privilege.COMMENT));
    }

    protected OntologyElement getOntologyElement(Element element) {
        if (element == null) {
            return null;
        }
        if (element instanceof Edge) {
            return getOntologyRelationshipFromElement((Edge) element);
        }
        if (element instanceof Vertex) {
            return getOntologyConceptFromElement((Vertex) element);
        }
        throw new VisalloException("Unexpected " + Element.class.getName() + " found " + element.getClass().getName());
    }

    protected OntologyElement getOntologyElement(ClientApiElement clientApiElement) {
        if (clientApiElement == null) {
            return null;
        }
        if (clientApiElement instanceof ClientApiEdge) {
            return getOntologyRelationshipFromElement((ClientApiEdge) clientApiElement);
        }
        if (clientApiElement instanceof ClientApiVertex) {
            return getOntologyConceptFromElement((ClientApiVertex) clientApiElement);
        }
        throw new VisalloException("Unexpected " + ClientApiVertex.class.getName() + " found " + clientApiElement.getClass().getName());
    }

    private Relationship getOntologyRelationshipFromElement(Edge e) {
        String label = e.getLabel();
        return getOntologyRelationshipFromElement(label);
    }

    private Relationship getOntologyRelationshipFromElement(ClientApiEdge e) {
        String label = e.getLabel();
        return getOntologyRelationshipFromElement(label);
    }

    private Relationship getOntologyRelationshipFromElement(String edgeLabel) {
        checkNotNull(edgeLabel, "Edge label cannot be null");
        Relationship relationship = ontologyRepository.getRelationshipByIRI(edgeLabel);
        checkNotNull(relationship, edgeLabel + " does not exist in ontology");
        return relationship;
    }

    private Concept getOntologyConceptFromElement(Vertex vertex) {
        String iri = VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex, CONCEPT_TYPE_THING);
        return getOntologyConcept(iri);
    }

    private Concept getOntologyConceptFromElement(ClientApiVertex vertex) {
        String iri = VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex, CONCEPT_TYPE_THING);
        return getOntologyConcept(iri);
    }

    private Concept getOntologyConcept(String conceptType) {
        if (conceptType == null) {
            return null;
        }
        return ontologyRepository.getConceptByIRI(conceptType);
    }
}
