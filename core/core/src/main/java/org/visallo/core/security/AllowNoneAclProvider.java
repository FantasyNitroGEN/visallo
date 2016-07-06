package org.visallo.core.security;

import com.google.inject.Inject;
import org.vertexium.Element;
import org.vertexium.Graph;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;

public class AllowNoneAclProvider extends ACLProvider {

    @Inject
    public AllowNoneAclProvider(Graph graph, UserRepository userRepository, OntologyRepository ontologyRepository) {
        super(graph, userRepository, ontologyRepository);
    }

    @Override
    public boolean canDeleteElement(Element element, User user) {
        return false;
    }

    @Override
    public boolean canDeleteProperty(Element element, String propertyKey, String propertyName, User user) {
        return false;
    }

    @Override
    public boolean canUpdateElement(Element element, User user) {
        return false;
    }

    @Override
    public boolean canUpdateProperty(Element element, String propertyKey, String propertyName, User user) {
        return false;
    }

    @Override
    public boolean canAddProperty(Element element, String propertyKey, String propertyName, User user) {
        return false;
    }
}
