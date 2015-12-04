package org.visallo.web.routes.element;

import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Element;
import org.vertexium.ElementType;
import org.vertexium.Graph;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.security.ACLProvider;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiElementAcl;

import javax.servlet.http.HttpServletRequest;

public abstract class ElementGetAcl implements ParameterizedHandler {
    private Graph graph;
    private OntologyRepository ontologyRepository;
    private ACLProvider aclProvider;
    private ElementType elementType;

    protected ElementGetAcl(Graph graph, OntologyRepository ontologyRepository, ACLProvider aclProvider,
                            ElementType elementType) {
        this.graph = graph;
        this.ontologyRepository = ontologyRepository;
        this.aclProvider = aclProvider;
        this.elementType = elementType;
    }

    @Handle
    public ClientApiElementAcl handle(
            HttpServletRequest request,
            @Required(name = "elementId") String elementId,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Element element;

        if (elementType == ElementType.VERTEX) {
            element = graph.getVertex(elementId, authorizations);
        } else if (elementType == ElementType.EDGE) {
            element = graph.getEdge(elementId, authorizations);
        } else {
            throw new VisalloException("Unrecognized element type: " + elementType.name());
        }

        if (element == null) {
            throw new VisalloResourceNotFoundException(
                    String.format("%s %s not found", elementType.name(), elementId), elementId);
        }

        return aclProvider.elementACL(element, user, ontologyRepository);
    }
}
