package org.visallo.web.routes.resource;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.web.VisalloResponse;

public class ResourceGet implements ParameterizedHandler {
    private final OntologyRepository ontologyRepository;

    @Inject
    public ResourceGet(final OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Handle
    public void handle(
            @Required(name = "id") String id,
            @Optional(name = "state") String state,
            VisalloResponse response
    ) throws Exception {
        Concept concept = ontologyRepository.getConceptByIRI(id);

        byte[] rawImg;
        if ("selected".equals(state) && concept.hasGlyphIconSelectedResource()) {
            rawImg = concept.getGlyphIconSelected();
        } else {
            rawImg = concept.getGlyphIcon();
        }

        if (rawImg == null || rawImg.length <= 0) {
            throw new VisalloResourceNotFoundException("Could not find resource with id: " + id);
        }

        // TODO change content type if we use this route for more than getting glyph icons
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "max-age=" + (5 * 60));
        response.write(rawImg);
    }
}
