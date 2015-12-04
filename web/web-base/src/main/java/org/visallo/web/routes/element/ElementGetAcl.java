package org.visallo.web.routes.element;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Element;
import org.vertexium.Graph;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.security.ACLProvider;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiElementAcl;

import javax.servlet.http.HttpServletRequest;

public class ElementGetAcl implements ParameterizedHandler {
    private Graph graph;
    private OntologyRepository ontologyRepository;
    private ACLProvider aclProvider;

    @Inject
    public ElementGetAcl(Graph graph, OntologyRepository ontologyRepository, ACLProvider aclProvider) {
        this.graph = graph;
        this.ontologyRepository = ontologyRepository;
        this.aclProvider = aclProvider;
    }

    @Handle
    public ClientApiElementAcl handle(
            HttpServletRequest request,
            @Required(name = "elementId") String elementId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        String type = request.getPathInfo().split("/")[1];
        Element element;

        if (type.equals("vertex")) {
            element = graph.getVertex(elementId, authorizations);
        } else if (type.equals("edge")) {
            element = graph.getEdge(elementId, authorizations);
        } else {
            throw new VisalloException("Unrecognized element type: " + type);
        }

        if (element == null) {
            throw new VisalloResourceNotFoundException(String.format("%s %s not found", type, elementId), elementId);
        }

        return aclProvider.elementACL(element, user, ontologyRepository);
    }
}
