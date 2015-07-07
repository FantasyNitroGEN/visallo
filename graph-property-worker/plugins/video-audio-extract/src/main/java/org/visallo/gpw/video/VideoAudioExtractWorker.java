package org.visallo.gpw.video;

import com.google.inject.Inject;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.MediaVisalloProperties;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.core.util.ProcessRunner;
import org.vertexium.Element;
import org.vertexium.Metadata;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.property.StreamingPropertyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Name("Video Audio Extract")
@Description("Extracts the audio stream from a video")
public class VideoAudioExtractWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VideoAudioExtractWorker.class);
    private static final String PROPERTY_KEY = "";
    private ProcessRunner processRunner;

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        File mp3File = File.createTempFile("audio_extract_", ".mp3");
        try {
            processRunner.execute(
                    "ffmpeg",
                    new String[]{
                            "-i", data.getLocalFile().getAbsolutePath(),
                            "-vn",
                            "-ar", "44100",
                            "-ab", "320k",
                            "-f", "mp3",
                            "-y",
                            mp3File.getAbsolutePath()
                    },
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();

            try (InputStream mp3FileIn = new FileInputStream(mp3File)) {
                StreamingPropertyValue spv = new StreamingPropertyValue(mp3FileIn, byte[].class);
                spv.searchIndex(false);
                Metadata metadata = new Metadata();
                metadata.add(VisalloProperties.MIME_TYPE.getPropertyName(), MediaVisalloProperties.MIME_TYPE_AUDIO_MP3, getVisibilityTranslator().getDefaultVisibility());
                MediaVisalloProperties.AUDIO_MP3.setProperty(m, spv, metadata, data.getProperty().getVisibility());
                m.save(getAuthorizations());
                getGraph().flush();

                getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), PROPERTY_KEY, MediaVisalloProperties.AUDIO_MP3.getPropertyName(), data.getPriority());
            }
        } finally {
            if (!mp3File.delete()) {
                LOGGER.warn("Could not delete %s", mp3File.getAbsolutePath());
            }
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(VisalloProperties.RAW.getPropertyName())) {
            return false;
        }
        String mimeType = VisalloProperties.MIME_TYPE_METADATA.getMetadataValue(property.getMetadata(), null);
        if (mimeType == null || !mimeType.startsWith("video")) {
            return false;
        }

        if (MediaVisalloProperties.AUDIO_MP3.hasProperty(element)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isLocalFileRequired() {
        return true;
    }

    @Inject
    public void setProcessRunner(ProcessRunner ffmpeg) {
        this.processRunner = ffmpeg;
    }
}
