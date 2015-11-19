package org.visallo.core.security;

import org.vertexium.Element;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.*;

import java.util.Collection;

public abstract class ACLProvider {
    public abstract boolean canDeleteElement(Element e, final User user);

    public abstract boolean canDeleteProperty(Element e, String propertyKey, String propertyName, final User user);

    public abstract boolean canUpdateElement(Element e, final User user);

    public abstract boolean canUpdateProperty(Element e, String propertyKey, String propertyName, final User user);

    public abstract boolean canDeleteElement(ClientApiElement e);

    public abstract boolean canDeleteProperty(ClientApiElement e, ClientApiProperty p);

    public abstract boolean canUpdateElement(ClientApiElement e);

    public abstract boolean canUpdateProperty(ClientApiElement e, ClientApiProperty p);

    public ClientApiObject appendACL(ClientApiObject clientApiObject) {
        if (clientApiObject instanceof ClientApiElement) {
            ClientApiElement apiElement = (ClientApiElement) clientApiObject;
            appendACL(apiElement);
        } else if (clientApiObject instanceof ClientApiWorkspaceVertices) {
            appendACL(((ClientApiWorkspaceVertices) clientApiObject).getVertices());
        } else if (clientApiObject instanceof ClientApiVertexMultipleResponse) {
            appendACL(((ClientApiVertexMultipleResponse) clientApiObject).getVertices());
        } else if (clientApiObject instanceof ClientApiEdgeMultipleResponse) {
            appendACL(((ClientApiEdgeMultipleResponse) clientApiObject).getEdges());
        } else if (clientApiObject instanceof ClientApiElementSearchResponse) {
            appendACL(((ClientApiElementSearchResponse) clientApiObject).getElements());
        } else if (clientApiObject instanceof ClientApiEdgeSearchResponse) {
            appendACL(((ClientApiEdgeSearchResponse) clientApiObject).getResults());
        } else if (clientApiObject instanceof ClientApiVertexEdges) {
            ClientApiVertexEdges vertexEdges = (ClientApiVertexEdges) clientApiObject;
            appendACL(vertexEdges);
        }

        return clientApiObject;
    }

    protected void appendACL(ClientApiElement apiElement) {
        for (ClientApiProperty property : apiElement.getProperties()) {
            property.setUpdateable(canUpdateProperty(apiElement, property));
            property.setDeleteable(canDeleteProperty(apiElement, property));
        }
        apiElement.setUpdateable(canUpdateElement(apiElement));
        apiElement.setDeleteable(canDeleteElement(apiElement));

        if (apiElement instanceof ClientApiEdgeWithVertexData) {
            appendACL(((ClientApiEdgeWithVertexData) apiElement).getSource());
            appendACL(((ClientApiEdgeWithVertexData) apiElement).getTarget());
        }
    }

    protected void appendACL(ClientApiVertexEdges edges) {
        for (ClientApiVertexEdges.Edge vertexEdge : edges.getRelationships()) {
            appendACL(vertexEdge.getRelationship());
            appendACL(vertexEdge.getVertex());
        }
    }

    public void appendACL(Collection<? extends ClientApiObject> clientApiObject) {
        for (ClientApiObject apiObject : clientApiObject) {
            appendACL(apiObject);
        }
    }
}
