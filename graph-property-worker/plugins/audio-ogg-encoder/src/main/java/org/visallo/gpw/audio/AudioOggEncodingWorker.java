package org.visallo.gpw.audio;

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
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Name("Audio OGG Encoding")
@Description("Encodes audio into the OGG format")
public class AudioOggEncodingWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(AudioOggEncodingWorker.class);
    private static final String PROPERTY_KEY = "";
    private ProcessRunner processRunner;

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        File mp4File = File.createTempFile("encode_ogg_", ".ogg");
        try {
            processRunner.execute(
                    "ffmpeg",
                    new String[]{
                            "-y", // overwrite output files
                            "-i", data.getLocalFile().getAbsolutePath(),
                            "-acodec", "libvorbis",
                            mp4File.getAbsolutePath()
                    },
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();

            try (InputStream mp4FileIn = new FileInputStream(mp4File)) {
                StreamingPropertyValue spv = new StreamingPropertyValue(mp4FileIn, byte[].class);
                spv.searchIndex(false);
                Metadata metadata = data.createPropertyMetadata();
                metadata.add(VisalloProperties.MIME_TYPE.getPropertyName(), MediaVisalloProperties.MIME_TYPE_AUDIO_OGG, getVisibilityTranslator().getDefaultVisibility());
                MediaVisalloProperties.AUDIO_OGG.setProperty(m, spv, metadata, data.getProperty().getVisibility());
                m.save(getAuthorizations());
                getGraph().flush();
                getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), PROPERTY_KEY, MediaVisalloProperties.AUDIO_OGG.getPropertyName(), data.getPriority());
            }
        } finally {
            if (!mp4File.delete()) {
                LOGGER.warn("could not delete %s", mp4File.getAbsolutePath());
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
        if (mimeType == null || !mimeType.startsWith("audio")) {
            return false;
        }

        if (MediaVisalloProperties.AUDIO_OGG.hasProperty(element)) {
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
