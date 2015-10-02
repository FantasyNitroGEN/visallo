package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.apache.hadoop.util.LimitInputStream;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Vertex;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.properties.MediaVisalloProperties;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.web.BadRequestException;
import org.visallo.web.VisalloResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public class VertexRaw implements ParameterizedHandler {
    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=([0-9]*)-([0-9]*)");

    private final Graph graph;

    @Inject
    public VertexRaw(final Graph graph) {
        this.graph = graph;
    }

    @Handle
    public InputStream handle(
            HttpServletRequest request,
            @Required(name = "graphVertexId") String graphVertexId,
            @Optional(name = "download", defaultValue = "false") boolean download,
            @Optional(name = "playback", defaultValue = "false") boolean playback,
            @Optional(name = "type") String type,
            Authorizations authorizations,
            VisalloResponse response
    ) throws Exception {
        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            throw new VisalloResourceNotFoundException("Could not find vertex with id: " + graphVertexId);
        }

        String fileName = VisalloProperties.FILE_NAME.getOnlyPropertyValue(artifactVertex);

        if (playback) {
            return handlePartialPlayback(request, response, artifactVertex, fileName, type);
        } else {
            String mimeType = getMimeType(artifactVertex);
            response.setContentType(mimeType);
            response.setMaxAge(VisalloResponse.EXPIRES_1_HOUR);
            String fileNameWithoutQuotes = fileName.replace('"', '\'');
            if (download) {
                response.addHeader("Content-Disposition", "attachment; filename=\"" + fileNameWithoutQuotes + "\"");
            } else {
                response.addHeader("Content-Disposition", "inline; filename=\"" + fileNameWithoutQuotes + "\"");
            }

            StreamingPropertyValue rawValue = VisalloProperties.RAW.getPropertyValue(artifactVertex);
            if (rawValue == null) {
                throw new VisalloResourceNotFoundException("Could not find raw on artifact: " + artifactVertex.getId());
            }
            return rawValue.getInputStream();
        }
    }

    private InputStream handlePartialPlayback(HttpServletRequest request, VisalloResponse response, Vertex artifactVertex, String fileName, String type) throws IOException {
        if (type == null) {
            throw new BadRequestException("type is required for partial playback");
        }

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

        return new LimitInputStream(in, partialLength);
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
