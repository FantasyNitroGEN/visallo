package org.visallo.core.security;

import org.vertexium.Element;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.*;

import java.util.Collection;

public abstract class ACLProvider {
    public abstract boolean canDeleteElement(Element e, User user);

    public abstract boolean canDeleteProperty(Element e, String propertyKey, String propertyName, User user);

    public abstract boolean canUpdateElement(Element e, User user);

    public abstract boolean canUpdateProperty(Element e, String propertyKey, String propertyName, User user);

    public abstract boolean canAddProperty(Element e, String propertyKey, String propertyName, User user);

    public abstract boolean canDeleteElement(ClientApiElement e, User user);

    public abstract boolean canDeleteProperty(ClientApiElement e, ClientApiProperty p, User user);

    public abstract boolean canUpdateElement(ClientApiElement e, User user);

    public abstract boolean canUpdateProperty(ClientApiElement e, ClientApiProperty p, User user);

    public abstract boolean canAddProperty(ClientApiElement e, ClientApiProperty p, User user);

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

    protected void appendACL(ClientApiElement apiElement, User user) {
        for (ClientApiProperty property : apiElement.getProperties()) {
            property.setUpdateable(canUpdateProperty(apiElement, property, user));
            property.setDeleteable(canDeleteProperty(apiElement, property, user));
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

    public void appendACL(Collection<? extends ClientApiObject> clientApiObject, User user) {
        for (ClientApiObject apiObject : clientApiObject) {
            appendACL(apiObject, user);
        }
    }
}
