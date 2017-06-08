package org.visallo.web.routes.resource;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.apache.commons.lang.StringUtils;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.StringUtil;
import org.visallo.web.VisalloResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

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
            User user,
            HttpServletRequest request,
            VisalloResponse response
    ) throws Exception {
        Glyph glyph = getConceptImage(id, state, user, user.getCurrentWorkspaceId());
        if (glyph == null || !glyph.isValid()) {
            throw new VisalloResourceNotFoundException("Could not find resource with id: " + id);
        }

        glyph.write(request, response);
    }

    private Glyph getConceptImage(String conceptIri, String state, User user, String workspaceId) {
        Concept concept = ontologyRepository.getConceptByIRI(conceptIri, user, workspaceId);
        if (concept == null) {
            return null;
        }

        Glyph glyph = getGlyph(concept, "selected".equals(state));


        if (glyph.isValid()) {
            return glyph;
        }

        String parentConceptIri = concept.getParentConceptIRI();
        if (parentConceptIri == null) {
            return null;
        }

        return getConceptImage(parentConceptIri, state, user, workspaceId);
    }

    private Glyph getGlyph(Concept concept, boolean isSelected) {
        Glyph glyph = null;
        if (isSelected && concept.hasGlyphIconSelectedResource()) {
            byte[] resource = concept.getGlyphIconSelected();
            if (resource != null) {
                glyph = new Image(resource);
            } else {
                glyph = new Path(concept.getGlyphIconSelectedFilePath());
            }
        } else if (concept.hasGlyphIconResource()) {
            byte[] resource = concept.getGlyphIcon();
            if (resource != null) {
                glyph = new Image(resource);
            } else {
                glyph = new Path(concept.getGlyphIconFilePath());
            }
        }

        return glyph;
    }

    interface Glyph {
        boolean isValid();
        void write(HttpServletRequest request, VisalloResponse response) throws IOException;
    }

    class Image implements Glyph {
        private byte[] img;
        public Image(byte[] img) {
            this.img = img;
        }
        public boolean isValid() {
            if (img == null || img.length <= 0) {
                return false;
            }
            return true;
        }
        public void write(HttpServletRequest request, VisalloResponse response) throws IOException {
            response.setContentType("image/png");
            response.setHeader("Cache-Control", "max-age=" + (5 * 60));
            response.write(img);
        }
    }

    class Path implements Glyph {
        private String path;
        public Path(String path) {
            this.path = path;
        }
        public boolean isValid() {
            if (StringUtils.isEmpty(path)) {
                return false;
            }
            return true;
        }
        public void write(HttpServletRequest request, VisalloResponse response) throws IOException {
            response.getHttpServletResponse().sendRedirect(path);
        }
    }
}
