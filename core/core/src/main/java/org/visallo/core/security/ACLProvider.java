package org.visallo.core.security;

import com.google.inject.Inject;
import org.vertexium.Element;
import org.vertexium.Graph;
import org.vertexium.Property;
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

    public abstract boolean canDeleteElement(Element element, User user);

    public abstract boolean canDeleteElement(ClientApiElement clientApiElement, User user);

    public abstract boolean canDeleteProperty(Element element, String propertyKey, String propertyName, User user);

    public abstract boolean canDeleteProperty(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user);

    public abstract boolean canUpdateElement(Element element, User user);

    public abstract boolean canUpdateElement(ClientApiElement clientApiElement, User user);

    public abstract boolean canUpdateProperty(Element element, String propertyKey, String propertyName, User user);

    public abstract boolean canUpdateProperty(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user);

    public abstract boolean canAddProperty(Element element, String propertyKey, String propertyName, User user);

    public abstract boolean canAddProperty(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user);

    public final void checkCanAddOrUpdateProperty(Element element, String propertyKey, String propertyName, User user)
            throws VisalloAccessDeniedException {
        boolean isUpdate = element.getProperty(propertyKey, propertyName) != null;
        boolean canAddOrUpdate = isUpdate
                ? internalCanUpdateProperty(element, propertyKey, propertyName, user)
                : internalCanAddProperty(element, propertyKey, propertyName, user);

        if (!canAddOrUpdate) {
            throw new VisalloAccessDeniedException(
                    propertyName + " cannot be added or updated due to ACL restriction", user, element.getId());
        }
    }

    public final void checkCanAddOrUpdateProperty(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user)
            throws VisalloAccessDeniedException {
        boolean isUpdate = clientApiElement.getProperty(propertyKey, propertyName) != null;
        boolean canAddOrUpdate = isUpdate
                ? internalCanUpdateProperty(clientApiElement, propertyKey, propertyName, user)
                : internalCanAddProperty(clientApiElement, propertyKey, propertyName, user);

        if (!canAddOrUpdate) {
            throw new VisalloAccessDeniedException(
                    propertyName + " cannot be added or updated due to ACL restriction", user, clientApiElement.getId());
        }
    }

    public final void checkCanDeleteProperty(Element element, String propertyKey, String propertyName, User user)
            throws VisalloAccessDeniedException {
        boolean canDelete = internalCanDeleteProperty(element, propertyKey, propertyName, user);

        if (!canDelete) {
            throw new VisalloAccessDeniedException(
                    propertyName + " cannot be deleted due to ACL restriction", user, element.getId());
        }
    }

    public final void checkCanDeleteProperty(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user)
            throws VisalloAccessDeniedException {
        boolean canDelete = internalCanDeleteProperty(clientApiElement, propertyKey, propertyName, user);

        if (!canDelete) {
            throw new VisalloAccessDeniedException(
                    propertyName + " cannot be deleted due to ACL restriction", user, clientApiElement.getId());
        }
    }

    public final ClientApiElementAcl elementACL(ClientApiElement clientApiElement, User user) {
        checkNotNull(clientApiElement, "clientApiElement is required");
        ClientApiElementAcl elementAcl = new ClientApiElementAcl();
        elementAcl.setAddable(true);
        elementAcl.setUpdateable(internalCanUpdateElement(clientApiElement, user));
        elementAcl.setDeleteable(internalCanDeleteElement(clientApiElement, user));

        List<ClientApiPropertyAcl> propertyAcls = elementAcl.getPropertyAcls();
        if (clientApiElement instanceof ClientApiVertex) {
            String iri = VisalloProperties.CONCEPT_TYPE.getPropertyValue(clientApiElement);
            while (iri != null) {
                Concept concept = ontologyRepository.getConceptByIRI(iri);
                if (concept == null) {
                    LOGGER.warn("Could not find concept: %s", iri);
                    break;
                }
                populatePropertyAcls(concept, clientApiElement, user, propertyAcls);
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
                populatePropertyAcls(relationship, clientApiElement, user, propertyAcls);
                iri = relationship.getParentIRI();
            }
        } else {
            throw new VisalloException("unsupported ClientApiElement class " + clientApiElement.getClass().getName());
        }
        return elementAcl;
    }

    public final ClientApiObject appendACL(ClientApiObject clientApiObject, User user) {
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
        } else if (clientApiObject instanceof ClientApiElementFindRelatedResponse) {
            appendACL(((ClientApiElementFindRelatedResponse) clientApiObject).getElements(), user);
        }

        return clientApiObject;
    }

    protected final boolean isComment(String propertyName) {
        return VisalloProperties.COMMENT.isSameName(propertyName);
    }

    protected final boolean isAuthor(Element element, String propertyKey, String propertyName, User user) {
        Property property = element.getProperty(propertyKey, propertyName);
        if (property != null) {
            String authorUserId = VisalloProperties.MODIFIED_BY_METADATA.getMetadataValue(property.getMetadata());
            return user.getUserId().equals(authorUserId);
        } else {
            return false;
        }
    }

    protected final boolean isAuthor(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user) {
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
        for (ClientApiProperty apiProperty : clientApiElement.getProperties()) {
            String key = apiProperty.getKey();
            String name = apiProperty.getName();
            apiProperty.setUpdateable(internalCanUpdateProperty(clientApiElement, key, name, user));
            apiProperty.setDeleteable(internalCanDeleteProperty(clientApiElement, key, name, user));
            apiProperty.setAddable(internalCanAddProperty(clientApiElement, key, name, user));
        }
        clientApiElement.setUpdateable(internalCanUpdateElement(clientApiElement, user));
        clientApiElement.setDeleteable(internalCanDeleteElement(clientApiElement, user));

        clientApiElement.setAcl(elementACL(clientApiElement, user));

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
            HasOntologyProperties hasOntologyProperties, ClientApiElement clientApiElement, User user,
            List<ClientApiPropertyAcl> propertyAcls
    ) {
        Collection<OntologyProperty> ontologyProperties = hasOntologyProperties.getProperties();
        Set<String> addedPropertyNames = new HashSet<>();
        for (OntologyProperty ontologyProperty : ontologyProperties) {
            String name = ontologyProperty.getTitle();
            for (ClientApiProperty property : clientApiElement.getProperties(name)) {
                propertyAcls.add(newClientApiPropertyAcl(clientApiElement, property.getKey(), name, user));
                addedPropertyNames.add(name);
            }
        }

        // for properties that don't exist on the clientApiElement, use the ontology property definition and omit the key.
        propertyAcls.addAll(
                ontologyProperties.stream()
                        .filter(ontologyProperty -> !addedPropertyNames.contains(ontologyProperty.getTitle()))
                        .map(ontologyProperty -> newClientApiPropertyAcl(
                                clientApiElement,
                                null,
                                ontologyProperty.getTitle(),
                                user
                        ))
                        .collect(Collectors.toList())
        );
    }

    private ClientApiPropertyAcl newClientApiPropertyAcl(ClientApiElement clientApiElement, String key, String name, User user) {
        ClientApiPropertyAcl propertyAcl = new ClientApiPropertyAcl();
        propertyAcl.setKey(key);
        propertyAcl.setName(name);
        propertyAcl.setAddable(internalCanAddProperty(clientApiElement, key, name, user));
        propertyAcl.setUpdateable(internalCanUpdateProperty(clientApiElement, key, name, user));
        propertyAcl.setDeleteable(internalCanDeleteProperty(clientApiElement, key, name, user));
        return propertyAcl;
    }

    private boolean internalCanDeleteElement(ClientApiElement clientApiElement, User user) {
        return hasPrivilege(user, Privilege.EDIT) && canDeleteElement(clientApiElement, user);
    }

    private boolean internalCanUpdateElement(ClientApiElement clientApiElement, User user) {
        return hasPrivilege(user, Privilege.EDIT) && canUpdateElement(clientApiElement, user);
    }

    private boolean internalCanDeleteProperty(Element element, String propertyKey, String propertyName, User user) {
        boolean canDelete = hasEditOrCommentPrivilege(propertyName, user)
                && canDeleteProperty(element, propertyKey, propertyName, user);
        if (canDelete && isComment(propertyName)) {
            canDelete = hasPrivilege(user, Privilege.COMMENT_DELETE_ANY) ||
                    (hasPrivilege(user, Privilege.COMMENT) && isAuthor(element, propertyKey, propertyName, user));
        }
        return canDelete;
    }

    private boolean internalCanDeleteProperty(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user) {
        boolean canDelete = hasEditOrCommentPrivilege(propertyName, user)
                && canDeleteProperty(clientApiElement, propertyKey, propertyName, user);
        if (canDelete && isComment(propertyName)) {
            canDelete = hasPrivilege(user, Privilege.COMMENT_DELETE_ANY) ||
                    (hasPrivilege(user, Privilege.COMMENT) && isAuthor(clientApiElement, propertyKey, propertyName, user));
        }
        return canDelete;
    }

    private boolean internalCanUpdateProperty(Element element, String propertyKey, String propertyName, User user) {
        boolean canUpdate = hasEditOrCommentPrivilege(propertyName, user)
                && canUpdateProperty(element, propertyKey, propertyName, user);
        if (canUpdate && isComment(propertyName)) {
            canUpdate = hasPrivilege(user, Privilege.COMMENT_EDIT_ANY) ||
                    (hasPrivilege(user, Privilege.COMMENT) && isAuthor(element, propertyKey, propertyName, user));
        }
        return canUpdate;
    }

    private boolean internalCanUpdateProperty(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user) {
        boolean canUpdate = hasEditOrCommentPrivilege(propertyName, user)
                && canUpdateProperty(clientApiElement, propertyKey, propertyName, user);
        if (canUpdate && isComment(propertyName)) {
            canUpdate = hasPrivilege(user, Privilege.COMMENT_EDIT_ANY) ||
                    (hasPrivilege(user, Privilege.COMMENT) && isAuthor(clientApiElement, propertyKey, propertyName, user));
        }
        return canUpdate;
    }

    private boolean internalCanAddProperty(Element element, String propertyKey, String propertyName, User user) {
        boolean canAdd = hasEditOrCommentPrivilege(propertyName, user)
                && canAddProperty(element, propertyKey, propertyName, user);
        if (canAdd && isComment(propertyName)) {
            canAdd = hasPrivilege(user, Privilege.COMMENT);
        }
        return canAdd;
    }

    private boolean internalCanAddProperty(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user) {
        boolean canAdd = hasEditOrCommentPrivilege(propertyName, user)
                && canAddProperty(clientApiElement, propertyKey, propertyName, user);
        if (canAdd && isComment(propertyName)) {
            canAdd = hasPrivilege(user, Privilege.COMMENT);
        }
        return canAdd;
    }

    private boolean hasEditOrCommentPrivilege(String propertyName, User user) {
        return hasPrivilege(user, Privilege.EDIT) || (isComment(propertyName) && hasPrivilege(user, Privilege.COMMENT));
    }
}
