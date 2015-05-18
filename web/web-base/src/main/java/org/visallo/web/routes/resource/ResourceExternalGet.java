package org.visallo.web.routes.resource;

import com.google.inject.Inject;
import org.visallo.core.config.Configuration;
import org.visallo.core.http.HttpRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.ImageUtils;
import org.visallo.web.BaseRequestHandler;
import org.apache.commons.io.IOUtils;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.property.StreamingPropertyValue;
import com.v5analytics.webster.HandlerChain;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ResourceExternalGet extends BaseRequestHandler {
    private final Graph graph;
    private final HttpRepository httpRepository;

    @Inject
    public ResourceExternalGet(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration,
            final HttpRepository httpRepository
    ) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
        this.httpRepository = httpRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String vertexId = getRequiredParameter(request, "vId");
        final String url = getRequiredParameter(request, "url");
        final int maxWidth = getRequiredParameterAsInt(request, "maxWidth");
        final int maxHeight = getRequiredParameterAsInt(request, "maxHeight");
        final int jpegQuality = getOptionalParameterInt(request, "jpegQuality", 80);
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        String propertyKey = getPropertyKey(url, maxWidth, maxHeight, jpegQuality);
        Vertex vertex = this.graph.getVertex(vertexId, authorizations);
        if (vertex == null) {
            respondWithNotFound(response, "Could not find vertex: " + vertexId);
            return;
        }

        InputStream in;
        StreamingPropertyValue cachedImageValue = VisalloProperties.CACHED_IMAGE.getPropertyValue(vertex, propertyKey);
        if (cachedImageValue != null) {
            in = cachedImageValue.getInputStream();
        } else {
            byte[] imageData = createAndSaveCachedImage(vertex, propertyKey, url, maxWidth, maxHeight, jpegQuality, authorizations);
            in = new ByteArrayInputStream(imageData);
        }

        ImageUtils.ImageFormat imageFormat = ImageUtils.getImageFormat(in);
        String imageMimeType = imageFormat.getImageMimeType();
        if (imageMimeType == null) {
            imageMimeType = "image";
        }

        response.setContentType(imageMimeType);
        response.addHeader("Content-Disposition", "inline; filename=thumbnail-" + maxWidth + "x" + maxHeight + ".jpg");
        setMaxAge(response, EXPIRES_1_HOUR);

        ServletOutputStream out = response.getOutputStream();
        IOUtils.copy(imageFormat.getPushBackIn(), out);
        out.close();
    }

    private byte[] createAndSaveCachedImage(Vertex vertex, String propertyKey, String url, int maxWidth, int maxHeight, int jpegQuality, Authorizations authorizations) throws IOException {
        byte[] imageData = getAndSaveImageData(vertex, url, authorizations);
        imageData = ImageUtils.resize(imageData, maxWidth, maxHeight, jpegQuality);

        StreamingPropertyValue value = new StreamingPropertyValue(new ByteArrayInputStream(imageData), byte[].class);
        value.store(true).searchIndex(false);
        ExistingElementMutation<Vertex> m = vertex.prepareMutation();
        VisalloProperties.CACHED_IMAGE.addPropertyValue(m, propertyKey, value, vertex.getVisibility());
        m.save(authorizations);
        return imageData;
    }

    private byte[] getAndSaveImageData(Vertex vertex, String url, Authorizations authorizations) throws IOException {
        String propertyKey = getPropertyKey(url, null, null, null);
        StreamingPropertyValue originalImage = VisalloProperties.CACHED_IMAGE.getPropertyValue(vertex, propertyKey);
        if (originalImage != null) {
            return IOUtils.toByteArray(originalImage.getInputStream());
        }
        byte[] imageData = httpRepository.get(url);
        StreamingPropertyValue value = new StreamingPropertyValue(new ByteArrayInputStream(imageData), byte[].class);
        value.store(true).searchIndex(false);
        ExistingElementMutation<Vertex> m = vertex.prepareMutation();
        VisalloProperties.CACHED_IMAGE.addPropertyValue(m, propertyKey, value, vertex.getVisibility());
        m.save(authorizations);
        return imageData;
    }

    private String getPropertyKey(String url, Integer maxWidth, Integer maxHeight, Integer jpegQuality) {
        String result = url;
        if (maxWidth != null) {
            result += "-" + Integer.toString(maxWidth);
        }
        if (maxHeight != null) {
            result += "-" + Integer.toString(maxHeight);
        }
        if (jpegQuality != null) {
            result += "-" + Integer.toString(jpegQuality);
        }
        return result;
    }
}

