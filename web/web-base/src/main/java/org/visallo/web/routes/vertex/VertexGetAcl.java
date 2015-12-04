package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import org.vertexium.ElementType;
import org.vertexium.Graph;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.security.ACLProvider;
import org.visallo.web.routes.element.ElementGetAcl;

public class VertexGetAcl extends ElementGetAcl {
    @Inject
    public VertexGetAcl(Graph graph, OntologyRepository ontologyRepository, ACLProvider aclProvider) {
        super(graph, ontologyRepository, aclProvider, ElementType.VERTEX);
    }
}
