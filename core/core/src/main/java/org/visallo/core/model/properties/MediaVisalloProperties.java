package org.visallo.core.model.properties;

import org.visallo.core.model.properties.types.StreamingVisalloProperty;
import org.visallo.core.model.properties.types.StreamingSingleValueVisalloProperty;
import org.visallo.core.model.properties.types.VideoTranscriptProperty;

/**
 * VisalloProperties for media files (video, images, etc.).
 */
public class MediaVisalloProperties {
    public static final String MIME_TYPE_VIDEO_MP4 = "video/mp4";
    public static final String MIME_TYPE_VIDEO_WEBM = "video/webm";
    public static final String MIME_TYPE_AUDIO_MP3 = "audio/mp3";
    public static final String MIME_TYPE_AUDIO_MP4 = "audio/mp4";
    public static final String MIME_TYPE_AUDIO_OGG = "audio/ogg";

    public static final String METADATA_VIDEO_FRAME_START_TIME = "http://visallo.org#videoFrameStartTime";

    public static final StreamingSingleValueVisalloProperty VIDEO_MP4 = new StreamingSingleValueVisalloProperty("http://visallo.org#video-mp4");
    public static final StreamingSingleValueVisalloProperty VIDEO_WEBM = new StreamingSingleValueVisalloProperty("http://visallo.org#video-webm");
    public static final StreamingSingleValueVisalloProperty AUDIO_MP3 = new StreamingSingleValueVisalloProperty("http://visallo.org#audio-mp3");
    public static final StreamingSingleValueVisalloProperty AUDIO_MP4 = new StreamingSingleValueVisalloProperty("http://visallo.org#audio-mp4");
    public static final StreamingSingleValueVisalloProperty AUDIO_OGG = new StreamingSingleValueVisalloProperty("http://visallo.org#audio-ogg");

    public static final VideoTranscriptProperty VIDEO_TRANSCRIPT = new VideoTranscriptProperty("http://visallo.org#videoTranscript");
    public static final StreamingVisalloProperty RAW_POSTER_FRAME = new StreamingVisalloProperty("http://visallo.org#rawPosterFrame");
    public static final StreamingSingleValueVisalloProperty VIDEO_PREVIEW_IMAGE = new StreamingSingleValueVisalloProperty("http://visallo.org#videoPreviewImage");
    public static final StreamingVisalloProperty VIDEO_FRAME = new StreamingVisalloProperty("http://visallo.org#videoFrame");

    private MediaVisalloProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
