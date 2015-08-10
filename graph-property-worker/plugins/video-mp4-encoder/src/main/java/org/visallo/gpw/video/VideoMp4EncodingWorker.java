package org.visallo.gpw.video;

import com.google.inject.Inject;
import org.vertexium.Element;
import org.vertexium.Metadata;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.ingest.graphProperty.VerifyResults;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.MediaVisalloProperties;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.IntegerVisalloProperty;
import org.visallo.core.util.FFprobeVideoFiltersUtil;
import org.visallo.core.util.ProcessRunner;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

@Name("Video MP4 Encoder")
@Description("Encodes video into MP4 format")
public class VideoMp4EncodingWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VideoMp4EncodingWorker.class);
    private static final String PROPERTY_KEY = "";
    private ProcessRunner processRunner;
    private IntegerVisalloProperty videoRotationProperty;

    @Override
    public VerifyResults verify() {
        VerifyResults verifyResults = super.verify();
        verifyResults.verifyRequiredPropertyIntent(getOntologyRepository(), "media.clockwiseRotation");
        if (verifyResults.verifyRequiredExecutable("ffmpeg")) {
            verifyFfmpegFeature(verifyResults);
        }
        verifyResults.verifyRequiredExecutable("qt-faststart");
        return verifyResults;
    }

    private void verifyFfmpegFeature(VerifyResults verifyResults) {
        String output = processRunner.executeToString("ffmpeg", new String[]{"-version"});
        if (!output.contains("enable-libx264")) {
            verifyResults.addFailure(new VerifyResults.GenericFailure("ffmpeg not compiled with 'libx264'"));
        }
        if (!output.contains("enable-libfdk-aac")) {
            verifyResults.addFailure(new VerifyResults.GenericFailure("ffmpeg not compiled with 'libfdk-aac'"));
        }
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        videoRotationProperty = new IntegerVisalloProperty(getOntologyRepository().getRequiredPropertyIRIByIntent("media.clockwiseRotation"));
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        File mp4File = File.createTempFile("encode_mp4_", ".mp4");
        File mp4RelocatedFile = File.createTempFile("relocated_mp4_", ".mp4");
        String[] ffmpegOptionsArray = prepareFFMPEGOptions(data, mp4File);
        try {
            processRunner.execute(
                    "ffmpeg",
                    ffmpegOptionsArray,
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            processRunner.execute(
                    "qt-faststart",
                    new String[]{
                            mp4File.getAbsolutePath(),
                            mp4RelocatedFile.getAbsolutePath()
                    },
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();

            try (InputStream mp4RelocatedFileIn = new FileInputStream(mp4RelocatedFile)) {
                StreamingPropertyValue spv = new StreamingPropertyValue(mp4RelocatedFileIn, byte[].class);
                spv.searchIndex(false);
                Metadata metadata = data.createPropertyMetadata();
                metadata.add(VisalloProperties.MIME_TYPE.getPropertyName(), MediaVisalloProperties.MIME_TYPE_VIDEO_MP4, getVisibilityTranslator().getDefaultVisibility());
                MediaVisalloProperties.VIDEO_MP4.setProperty(m, spv, metadata, data.getProperty().getVisibility());
                m.save(getAuthorizations());
                getGraph().flush();
                getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), PROPERTY_KEY, MediaVisalloProperties.VIDEO_MP4.getPropertyName(), data.getPriority());
            }
        } finally {
            if (!mp4File.delete()) {
                LOGGER.warn("Could not delete %s" + mp4File.getAbsolutePath());
            }
            if (!mp4RelocatedFile.delete()) {
                LOGGER.warn("Could not delete %s" + mp4RelocatedFile.getAbsolutePath());
            }
        }
    }

    public String[] prepareFFMPEGOptions(GraphPropertyWorkData data, File mp4File) {
        ArrayList<String> ffmpegOptionsList = new ArrayList<>();

        ffmpegOptionsList.add("-y");
        ffmpegOptionsList.add("-i");
        ffmpegOptionsList.add(data.getLocalFile().getAbsolutePath());
        ffmpegOptionsList.add("-vcodec");
        ffmpegOptionsList.add("libx264");
        ffmpegOptionsList.add("-vprofile");
        ffmpegOptionsList.add("high");
        ffmpegOptionsList.add("-preset");
        ffmpegOptionsList.add("slow");
        ffmpegOptionsList.add("-b:v");
        ffmpegOptionsList.add("500k");
        ffmpegOptionsList.add("-maxrate");
        ffmpegOptionsList.add("500k");
        ffmpegOptionsList.add("-bufsize");
        ffmpegOptionsList.add("1000k");

        Integer videoRotation = videoRotationProperty.getOnlyPropertyValue(data.getElement());
        String[] ffmpegVideoFilterOptions = FFprobeVideoFiltersUtil.getFFmpegVideoFilterOptions(videoRotation);
        if (ffmpegVideoFilterOptions != null) {
            ffmpegOptionsList.add(ffmpegVideoFilterOptions[0]);
            ffmpegOptionsList.add(ffmpegVideoFilterOptions[1]);
        }

        ffmpegOptionsList.add("-threads");
        ffmpegOptionsList.add("0");
        ffmpegOptionsList.add("-acodec");
        ffmpegOptionsList.add("libfdk_aac");
        ffmpegOptionsList.add("-b:a");
        ffmpegOptionsList.add("128k");
        ffmpegOptionsList.add("-f");
        ffmpegOptionsList.add("mp4");
        ffmpegOptionsList.add(mp4File.getAbsolutePath());
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

        if (MediaVisalloProperties.VIDEO_MP4.hasProperty(element)) {
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
