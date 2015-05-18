package org.visallo.gpw.video;

import com.google.inject.Inject;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.MediaVisalloProperties;
import org.visallo.core.model.properties.types.DoubleVisalloProperty;
import org.visallo.core.model.properties.types.IntegerVisalloProperty;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.core.util.ProcessRunner;
import org.visallo.core.util.FFprobeRotationUtil;
import org.vertexium.Element;
import org.vertexium.Metadata;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.property.StreamingPropertyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

@Name("Video Poster Frame")
@Description("Gets a video poster frame by extracting a frame from the video")
public class VideoPosterFrameWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VideoPosterFrameWorker.class);
    private static final String PROPERTY_KEY = VideoPosterFrameWorker.class.getName();
    private ProcessRunner processRunner;
    private DoubleVisalloProperty durationProperty;
    private IntegerVisalloProperty videoRotationProperty;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        durationProperty = new DoubleVisalloProperty(getOntologyRepository().getRequiredPropertyIRIByIntent("media.duration"));
        videoRotationProperty = new IntegerVisalloProperty(getOntologyRepository().getRequiredPropertyIRIByIntent("media.clockwiseRotation"));
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        File videoPosterFrameFile = File.createTempFile("video_poster_frame", ".png");
        String[] ffmpegOptionsArray = prepareFFMPEGOptions(data, videoPosterFrameFile);
        try {
            processRunner.execute(
                    "ffmpeg",
                    ffmpegOptionsArray,
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            if (videoPosterFrameFile.length() == 0) {
                throw new RuntimeException("Poster frame not created. Zero length file detected. (from: " + data.getLocalFile().getAbsolutePath() + ")");
            }

            try (InputStream videoPosterFrameFileIn = new FileInputStream(videoPosterFrameFile)) {
                ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();

                StreamingPropertyValue spv = new StreamingPropertyValue(videoPosterFrameFileIn, byte[].class);
                spv.searchIndex(false);
                Metadata metadata = new Metadata();
                metadata.add(VisalloProperties.MIME_TYPE.getPropertyName(), "image/png", getVisibilityTranslator().getDefaultVisibility());
                MediaVisalloProperties.RAW_POSTER_FRAME.addPropertyValue(m, PROPERTY_KEY, spv, metadata, data.getProperty().getVisibility());
                m.save(getAuthorizations());
                getGraph().flush();
            }
        } finally {
            if (!videoPosterFrameFile.delete()) {
                LOGGER.warn("Could not delete %s", videoPosterFrameFile.getAbsolutePath());
            }
        }
    }

    private String[] prepareFFMPEGOptions(GraphPropertyWorkData data, File videoPosterFrameFile) {
        ArrayList<String> ffmpegOptionsList = new ArrayList<>();
        Double duration = durationProperty.getFirstPropertyValue(data.getElement(), null);

        if (duration != null) {
            ffmpegOptionsList.add("-itsoffset");
            ffmpegOptionsList.add("-" + (duration / 3.0));
        }

        ffmpegOptionsList.add("-i");
        ffmpegOptionsList.add(data.getLocalFile().getAbsolutePath());
        ffmpegOptionsList.add("-vcodec");
        ffmpegOptionsList.add("png");
        ffmpegOptionsList.add("-vframes");
        ffmpegOptionsList.add("1");
        ffmpegOptionsList.add("-an");
        ffmpegOptionsList.add("-f");
        ffmpegOptionsList.add("rawvideo");

        Integer videoRotation = videoRotationProperty.getOnlyPropertyValue(data.getElement());
        if (videoRotation != null) {
            //Scale.
            //Will not force conversion to 720:480 aspect ratio, but will resize video with original aspect ratio.
            if (videoRotation == 0 || videoRotation == 180) {
                ffmpegOptionsList.add("-s");
                ffmpegOptionsList.add("720x480");
            } else if (videoRotation == 90 || videoRotation == 270) {
                ffmpegOptionsList.add("-s");
                ffmpegOptionsList.add("480x720");
            }

            String[] ffmpegRotationOptions = FFprobeRotationUtil.createFFMPEGRotationOptions(videoRotation);
            //Rotate
            if (ffmpegRotationOptions != null) {
                ffmpegOptionsList.add(ffmpegRotationOptions[0]);
                ffmpegOptionsList.add(ffmpegRotationOptions[1]);
            }
        }

        ffmpegOptionsList.add("-y");
        ffmpegOptionsList.add(videoPosterFrameFile.getAbsolutePath());

        return ffmpegOptionsList.toArray(new String[ffmpegOptionsList.size()]);
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
