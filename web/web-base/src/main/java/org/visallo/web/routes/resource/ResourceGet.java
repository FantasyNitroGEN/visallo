package org.visallo.web.routes.resource;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.apache.commons.lang.StringUtils;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.RGBImageFilter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            @Optional(name = "tint") String tint,
            User user,
            HttpServletRequest request,
            VisalloResponse response
    ) throws Exception {
        Glyph glyph = getConceptImage(id, state, user, user.getCurrentWorkspaceId());
        if (glyph == null || !glyph.isValid()) {
            throw new VisalloResourceNotFoundException("Could not find resource with id: " + id);
        }

        response.setContentType("image/png");
        response.setHeader("Cache-Control", "max-age=" + (5 * 60));
        glyph.write(tint, request, response);
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
        void write(String tint, HttpServletRequest request, VisalloResponse response) throws IOException;
    }

    abstract class AbstractGlyph implements Glyph {
        private int[] convert(String tint) {
            Pattern hexPattern = Pattern.compile("^#.*$");
            Pattern rgbPattern = Pattern.compile("^\\s*rgb\\((\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)\\s*$");
            if (tint != null) {
                if (hexPattern.matcher(tint).matches()) {
                    int hex = Integer.parseInt(tint.replace("#", ""), 16);
                    int r = (hex >> 16) & 0xff;
                    int g = (hex >> 8) & 0xff;
                    int b = (hex >> 0) & 0xff;
                    return new int[]{r, g, b};
                }

                Matcher m = rgbPattern.matcher(tint);
                if (m.matches()) {
                    return new int[]{
                        Integer.parseInt(m.group(1)),
                        Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3))
                    };
                }
            }
            return null;
        }

        void write(BufferedImage bufferedImage, String tint, OutputStream outputStream) {
            int[] tintColor = convert(tint);
            if (tintColor != null && bufferedImage.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
                ImageFilter filter = new RGBImageFilter() {
                    @Override
                    public int filterRGB(int x, int y, int rgb) {
                        int a = (rgb >> 24) & 0xff;
                        int r = tintColor[0];
                        int g = tintColor[1];
                        int b = tintColor[2];
                        return a << 24 | r << 16 | g << 8 | b;
                    }
                };

                FilteredImageSource filteredImageSource = new FilteredImageSource(bufferedImage.getSource(), filter);
                java.awt.Image image = Toolkit.getDefaultToolkit().createImage(filteredImageSource);

                int width = image.getWidth(null);
                int height = image.getHeight(null);
                bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                Graphics g = bufferedImage.getGraphics();
                g.drawImage(image, 0, 0, null);
            }
            try {
                ImageIO.write(bufferedImage, "png", outputStream);
            } catch (IOException e) {
                throw new VisalloException("Unable to tint image", e);
            }
        }
    }

    class Image extends AbstractGlyph {
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
        public void write(String tint, HttpServletRequest request, VisalloResponse response) throws IOException {
            try (ByteArrayInputStream is = new ByteArrayInputStream(img)) {
                BufferedImage bufferedImage = ImageIO.read(is);
                write(bufferedImage, tint, response.getOutputStream());
            }
        }
    }

    class Path extends AbstractGlyph {
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
        public void write(String tint, HttpServletRequest request, VisalloResponse response) throws IOException {
            ServletContext servletContext = request.getServletContext();
            try (InputStream openStream = servletContext.getResourceAsStream(path)) {
                BufferedImage base = ImageIO.read(openStream);
                write(base, tint, response.getOutputStream());
            }
        }
    }
}
