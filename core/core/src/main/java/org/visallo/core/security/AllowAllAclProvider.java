package org.visallo.core.security;

import com.google.inject.Inject;
import org.vertexium.Element;
import org.vertexium.Graph;
import org.visallo.core.model.ontology.OntologyElement;
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
    public boolean canDeleteElement(Element element, OntologyElement ontologyElement, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canDeleteElement(ClientApiElement clientApiElement, OntologyElement ontologyElement, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canDeleteProperty(Element element, OntologyElement ontologyElement, String propertyKey, String propertyName, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canDeleteProperty(ClientApiElement clientApiElement, OntologyElement ontologyElement, String propertyKey, String propertyName, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canUpdateElement(Element element, OntologyElement ontologyElement, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canUpdateElement(ClientApiElement clientApiElement, OntologyElement ontologyElement, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canUpdateProperty(Element element, OntologyElement ontologyElement, String propertyKey, String propertyName, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canUpdateProperty(ClientApiElement clientApiElement, OntologyElement ontologyElement, String propertyKey, String propertyName, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canAddProperty(Element element, OntologyElement ontologyElement, String propertyKey, String propertyName, User user, String workspaceId) {
        return true;
    }

    @Override
    public boolean canAddProperty(ClientApiElement clientApiElement, OntologyElement ontologyElement, String propertyKey, String propertyName, User user, String workspaceId) {
        return true;
    }
}
