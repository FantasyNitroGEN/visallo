package org.visallo.web.product.graph;

import com.google.inject.Inject;
import com.v5analytics.webster.Handler;
import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.model.IRI;
import org.vertexium.Authorizations;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;

import javax.servlet.ServletContext;
import java.io.InputStream;

@Name("Product: Graph")
@Description("Graph visualization")
public class GraphWebAppPlugin implements WebAppPlugin {
    private final OntologyRepository ontologyRepository;
    private final UserRepository userRepository;
    private final AuthorizationRepository authorizationRepository;

    @Inject
    public GraphWebAppPlugin(
            OntologyRepository ontologyRepository,
            UserRepository userRepository,
            AuthorizationRepository authorizationRepository
    ) {
        this.ontologyRepository = ontologyRepository;
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
    }

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {

        app.registerJavaScript("/org/visallo/web/product/graph/plugin.js");

        app.registerCompiledJavaScript("/org/visallo/web/product/graph/dist/Graph.js");
        app.registerCompiledJavaScript("/org/visallo/web/product/graph/dist/EdgeLabel.js");
        app.registerCompiledJavaScript("/org/visallo/web/product/graph/dist/SnapToGrid.js");
        app.registerCompiledJavaScript("/org/visallo/web/product/graph/dist/FindPathPopoverContainer.js");
        app.registerCompiledJavaScript("/org/visallo/web/product/graph/dist/actions-impl.js");

        app.registerCompiledWebWorkerJavaScript("/org/visallo/web/product/graph/dist/plugin-worker.js");
        app.registerCompiledWebWorkerJavaScript("/org/visallo/web/product/graph/dist/store-changes.js");

        app.registerLess("/org/visallo/web/product/graph/css.less");
        app.registerResourceBundle("/org/visallo/web/product/graph/messages.properties");
        app.registerFile("/org/visallo/web/product/graph/select-arrow.png", "image/png");

        ensureOntologyDefined();
    }

    private void ensureOntologyDefined() {
        if (ontologyRepository.isOntologyDefined(GraphProductOntology.IRI)) {
            return;
        }

        try (InputStream graphOwl = GraphWebAppPlugin.class.getResourceAsStream("graph.owl")) {
            byte[] inFileData = IOUtils.toByteArray(graphOwl);
            IRI graphIRI = IRI.create(GraphProductOntology.IRI);
            Authorizations authorizations = authorizationRepository.getGraphAuthorizations(this.userRepository.getSystemUser());
            ontologyRepository.importFileData(inFileData, graphIRI, null, authorizations);
        } catch (Exception e) {
            throw new VisalloException("Could not read graph.owl file", e);
        }

    }
}
