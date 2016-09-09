package org.visallo.core.security;

import com.google.inject.Inject;
import org.vertexium.Element;
import org.vertexium.Graph;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.PrivilegeRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiElement;

public class AllowAllAclProvider extends ACLProvider {
    @Inject
    public AllowAllAclProvider(
            Graph graph,
            UserRepository userRepository,
            OntologyRepository ontologyRepository,
            PrivilegeRepository privilegeRepository
    ) {
        super(graph, userRepository, ontologyRepository, privilegeRepository);
    }

    @Override
    public boolean canDeleteElement(Element element, User user) {
        return true;
    }

    @Override
    public boolean canDeleteElement(ClientApiElement clientApiElement, User user) {
        return true;
    }

    @Override
    public boolean canDeleteProperty(Element element, String propertyKey, String propertyName, User user) {
        return true;
    }

    @Override
    public boolean canDeleteProperty(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user) {
        return true;
    }

    @Override
    public boolean canUpdateElement(Element element, User user) {
        return true;
    }

    @Override
    public boolean canUpdateElement(ClientApiElement clientApiElement, User user) {
        return true;
    }

    @Override
    public boolean canUpdateProperty(Element element, String propertyKey, String propertyName, User user) {
        return true;
    }

    @Override
    public boolean canUpdateProperty(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user) {
        return true;
    }

    @Override
    public boolean canAddProperty(Element element, String propertyKey, String propertyName, User user) {
        return true;
    }

    @Override
    public boolean canAddProperty(ClientApiElement clientApiElement, String propertyKey, String propertyName, User user) {
        return true;
    }
}
