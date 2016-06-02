package org.visallo.core.security;

import org.vertexium.Element;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiProperty;

public class AllowAllAclProvider extends ACLProvider {
    @Override
    public boolean canDeleteElement(Element element, User user) {
        return true;
    }

    @Override
    public boolean canDeleteProperty(Element element, String propertyKey, String propertyName, User user) {
        return true;
    }

    @Override
    public boolean canUpdateElement(Element element, User user) {
        return true;
    }

    @Override
    public boolean canUpdateProperty(Element element, String propertyKey, String propertyName, User user) {
        return true;
    }

    @Override
    public boolean canAddProperty(Element element, String propertyKey, String propertyName, User user) {
        return true;
    }

    @Override
    public boolean canDeleteElement(ClientApiElement element, User user) {
        return true;
    }

    @Override
    public boolean canDeleteProperty(ClientApiElement element, ClientApiProperty p, User user) {
        return true;
    }

    @Override
    public boolean canUpdateElement(ClientApiElement element, User user) {
        return true;
    }

    @Override
    public boolean canUpdateProperty(ClientApiElement element, ClientApiProperty p, User user) {
        return true;
    }

    @Override
    public boolean canAddProperty(ClientApiElement element, ClientApiProperty p, User user) {
        return true;
    }
}
