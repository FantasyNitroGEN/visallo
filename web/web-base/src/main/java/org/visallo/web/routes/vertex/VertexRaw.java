package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.utils.UrlUtils;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.MediaVisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BaseRequestHandler;
import org.apache.commons.io.IOUtils;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.property.StreamingPropertyValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public class VertexRaw extends BaseRequestHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VertexRaw.class);
    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=([0-9]*)-([0-9]*)");

    private final Graph graph;

    @Inject
    public VertexRaw(
            final Graph graph,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        boolean download = getOptionalParameter(request, "download") != null;
        boolean playback = getOptionalParameter(request, "playback") != null;

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        String graphVertexId = UrlUtils.urlDecode(getAttributeString(request, "graphVertexId"));

        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            respondWithNotFound(response);
            return;
        }

        String fileName = VisalloProperties.FILE_NAME.getOnlyPropertyValue(artifactVertex);

        if (playback) {
            handlePartialPlayback(request, response, artifactVertex, fileName);
        } else {
            String mimeType = getMimeType(artifactVertex);
            response.setContentType(mimeType);
            setMaxAge(response, EXPIRES_1_HOUR);
            String fileNameWithoutQuotes = fileName.replace('"', '\'');
            if (download) {
                response.addHeader("Content-Disposition", "attachment; filename=\"" + fileNameWithoutQuotes + "\"");
            } else {
                response.addHeader("Content-Disposition", "inline; filename=\"" + fileNameWithoutQuotes + "\"");
            }

            StreamingPropertyValue rawValue = VisalloProperties.RAW.getPropertyValue(artifactVertex);
            if (rawValue == null) {
                LOGGER.warn("Could not find raw on artifact: %s", artifactVertex.getId());
                respondWithNotFound(response);
                return;
            }
            try (InputStream in = rawValue.getInputStream()) {
                IOUtils.copy(in, response.getOutputStream());
            }
        }

        chain.next(request, response);
    }

    private void handlePartialPlayback(HttpServletRequest request, HttpServletResponse response, Vertex artifactVertex, String fileName) throws IOException {
        String type = getRequiredParameter(request, "type");

        InputStream in;
        Long totalLength;
        long partialStart = 0;
        Long partialEnd = null;
        String range = request.getHeader("Range");

        if (range != null) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

            Matcher m = RANGE_PATTERN.matcher(range);
            if (m.matches()) {
                partialStart = Long.parseLong(m.group(1));
                if (m.group(2).length() > 0) {
                    partialEnd = Long.parseLong(m.group(2));
                }
            }
        }

        response.setCharacterEncoding(null);
        response.setContentType(type);
        response.addHeader("Content-Disposition", "attachment; filename=" + fileName);

        StreamingPropertyValue mediaPropertyValue = getStreamingPropertyValue(artifactVertex, type);

        totalLength = mediaPropertyValue.getLength();
        in = mediaPropertyValue.getInputStream();

        if (partialEnd == null) {
            partialEnd = totalLength;
        }

        // Ensure that the last byte position is less than the instance-length
        partialEnd = Math.min(partialEnd, totalLength - 1);
        long partialLength = totalLength;

        if (range != null) {
            partialLength = partialEnd - partialStart + 1;
            response.addHeader("Content-Range", "bytes " + partialStart + "-" + partialEnd + "/" + totalLength);
            if (partialStart > 0) {
                in.skip(partialStart);
            }
        }

        response.addHeader("Content-Length", "" + partialLength);

        OutputStream out = response.getOutputStream();
        copy(in, out, partialLength);

        response.flushBuffer();
    }

    private StreamingPropertyValue getStreamingPropertyValue(Vertex artifactVertex, String type) {
        StreamingPropertyValue mediaPropertyValue;
        if (MediaVisalloProperties.MIME_TYPE_AUDIO_MP4.equals(type)) {
            mediaPropertyValue = MediaVisalloProperties.AUDIO_MP4.getPropertyValue(artifactVertex);
            checkNotNull(mediaPropertyValue, String.format("Could not find %s property on artifact %s", MediaVisalloProperties.MIME_TYPE_AUDIO_MP4, artifactVertex.getId()));
        } else if (MediaVisalloProperties.MIME_TYPE_AUDIO_OGG.equals(type)) {
            mediaPropertyValue = MediaVisalloProperties.AUDIO_OGG.getPropertyValue(artifactVertex);
            checkNotNull(mediaPropertyValue, String.format("Could not find %s property on artifact %s", MediaVisalloProperties.MIME_TYPE_AUDIO_OGG, artifactVertex.getId()));
        } else if (MediaVisalloProperties.MIME_TYPE_VIDEO_MP4.equals(type)) {
            mediaPropertyValue = MediaVisalloProperties.VIDEO_MP4.getPropertyValue(artifactVertex);
            checkNotNull(mediaPropertyValue, String.format("Could not find %s property on artifact %s", MediaVisalloProperties.MIME_TYPE_VIDEO_MP4, artifactVertex.getId()));
        } else if (MediaVisalloProperties.MIME_TYPE_VIDEO_WEBM.equals(type)) {
            mediaPropertyValue = MediaVisalloProperties.VIDEO_WEBM.getPropertyValue(artifactVertex);
            checkNotNull(mediaPropertyValue, String.format("Could not find %s property on artifact %s", MediaVisalloProperties.MIME_TYPE_VIDEO_WEBM, artifactVertex.getId()));
        } else {
            throw new VisalloException("Invalid video type: " + type);
        }
        return mediaPropertyValue;
    }

    private void copy(InputStream in, OutputStream out, Long length) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while (length > 0 && (read = in.read(buffer, 0, (int) Math.min(length, buffer.length))) > 0) {
            out.write(buffer, 0, read);
            length -= read;
        }
    }

    private String getMimeType(Vertex artifactVertex) {
        String mimeType = VisalloProperties.MIME_TYPE.getOnlyPropertyValue(artifactVertex);
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }
}
