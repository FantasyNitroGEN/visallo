package org.visallo.gpw.video;

import com.google.inject.Inject;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.ingest.graphProperty.VerifyResults;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.MediaVisalloProperties;
import org.visallo.core.model.properties.types.IntegerVisalloProperty;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.core.util.ProcessRunner;
import org.visallo.core.util.FFprobeVideoFiltersUtil;
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

@Name("Video WebM Encoder")
@Description("Encodes video into WebM format")
public class VideoWebMEncodingWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VideoWebMEncodingWorker.class);
    private ProcessRunner processRunner;
    private IntegerVisalloProperty videoRotationProperty;

    @Override
    public VerifyResults verify() {
        VerifyResults verifyResults = super.verify();
        verifyResults.verifyRequiredPropertyIntent(getOntologyRepository(), "media.clockwiseRotation");
        if (verifyResults.verifyRequiredExecutable("ffmpeg")) {
            verifyFfmpegFeature(verifyResults);
        }
        return verifyResults;
    }

    private void verifyFfmpegFeature(VerifyResults verifyResults) {
        String output = processRunner.executeToString("ffmpeg", new String[]{"-version"});
        if (!output.contains("enable-libvpx")) {
            verifyResults.addFailure(new VerifyResults.GenericFailure("ffmpeg not compiled with 'libvpx'"));
        }
        if (!output.contains("enable-libvorbis")) {
            verifyResults.addFailure(new VerifyResults.GenericFailure("ffmpeg not compiled with 'libvorbis'"));
        }
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        videoRotationProperty = new IntegerVisalloProperty(getOntologyRepository().getRequiredPropertyIRIByIntent("media.clockwiseRotation"));
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        File webmFile = File.createTempFile("encode_webm_", ".webm");
        String[] ffmpegOptionsArray = prepareFFMPEGOptions(data, webmFile);
        try {
            processRunner.execute(
                    "ffmpeg",
                    ffmpegOptionsArray,
                    null,
                    data.getLocalFile().getAbsolutePath() + ": "
            );

            ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();

            try (InputStream webmFileIn = new FileInputStream(webmFile)) {
                StreamingPropertyValue spv = new StreamingPropertyValue(webmFileIn, byte[].class);
                spv.searchIndex(false);
                Metadata metadata = new Metadata();
                metadata.add(VisalloProperties.MIME_TYPE.getPropertyName(), MediaVisalloProperties.MIME_TYPE_VIDEO_WEBM, getVisibilityTranslator().getDefaultVisibility());
                MediaVisalloProperties.VIDEO_WEBM.setProperty(m, spv, metadata, data.getProperty().getVisibility());
                m.save(getAuthorizations());
            }
        } finally {
            if (!webmFile.delete()) {
                LOGGER.warn("Could not delete %s", webmFile.getAbsolutePath());
            }
        }
    }

    private String[] prepareFFMPEGOptions(GraphPropertyWorkData data, File webmFile) {
        ArrayList<String> ffmpegOptionsList = new ArrayList<>();

        ffmpegOptionsList.add("-y");
        ffmpegOptionsList.add("-i");
        ffmpegOptionsList.add(data.getLocalFile().getAbsolutePath());
        ffmpegOptionsList.add("-vcodec");
        ffmpegOptionsList.add("libvpx");
        ffmpegOptionsList.add("-b:v");
        ffmpegOptionsList.add("600k");
        ffmpegOptionsList.add("-qmin");
        ffmpegOptionsList.add("10");
        ffmpegOptionsList.add("-qmax");
        ffmpegOptionsList.add("42");
        ffmpegOptionsList.add("-maxrate");
        ffmpegOptionsList.add("500k");
        ffmpegOptionsList.add("-bufsize");
        ffmpegOptionsList.add("1000k");
        ffmpegOptionsList.add("-threads");
        ffmpegOptionsList.add("2");

        Integer videoRotation = videoRotationProperty.getOnlyPropertyValue(data.getElement());
        String[] ffmpegVideoFilterOptions = FFprobeVideoFiltersUtil.getFFmpegVideoFilterOptions(videoRotation);
        if (ffmpegVideoFilterOptions != null) {
            ffmpegOptionsList.add(ffmpegVideoFilterOptions[0]);
            ffmpegOptionsList.add(ffmpegVideoFilterOptions[1]);
        }

        ffmpegOptionsList.add("-acodec");
        ffmpegOptionsList.add("libvorbis");
        ffmpegOptionsList.add("-map");
        ffmpegOptionsList.add("0");
        ffmpegOptionsList.add("-map");
        ffmpegOptionsList.add("-0:s");
        ffmpegOptionsList.add("-f");
        ffmpegOptionsList.add("webm");
        ffmpegOptionsList.add(webmFile.getAbsolutePath());
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

        if (MediaVisalloProperties.VIDEO_WEBM.hasProperty(element)) {
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
