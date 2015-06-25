package org.visallo.gpw.video;

import com.google.common.io.Files;
import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.vertexium.*;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.ingest.video.VideoFrameInfo;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import org.visallo.core.model.properties.MediaVisalloProperties;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.DoubleSingleValueVisalloProperty;
import org.visallo.core.model.properties.types.DoubleVisalloProperty;
import org.visallo.core.model.properties.types.IntegerVisalloProperty;
import org.visallo.core.security.VisalloVisibility;
import org.visallo.core.util.FFprobeVideoFiltersUtil;
import org.visallo.core.util.ProcessRunner;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.vertexium.util.IterableUtils.toList;

@Name("Video Frame Extract")
@Description("Extracts frames of the video for image processing")
public class VideoFrameExtractGraphPropertyWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VideoFrameExtractGraphPropertyWorker.class);
    private ProcessRunner processRunner;
    private IntegerVisalloProperty videoRotationProperty;
    private DoubleVisalloProperty videoDurationProperty;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        getAuthorizationRepository().addAuthorizationToGraph(VideoFrameInfo.VISIBILITY_STRING);
        videoRotationProperty = new IntegerVisalloProperty(getOntologyRepository().getRequiredPropertyIRIByIntent("media.clockwiseRotation"));
        videoDurationProperty = new DoubleVisalloProperty(getOntologyRepository().getRequiredPropertyIRIByIntent("media.duration"));
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        Integer videoRotation = videoRotationProperty.getOnlyPropertyValue(data.getElement(), 0);
        Double videoDuration = videoDurationProperty.getOnlyPropertyValue(data.getElement());
        Visibility newVisibility = new VisalloVisibility(VisalloVisibility.and(getVisibilityTranslator().toVisibilityNoSuperUser(data.getVisibilityJson()), VideoFrameInfo.VISIBILITY_STRING)).getVisibility();

        Pattern fileNamePattern = Pattern.compile("image-([0-9]+)\\.png");
        File tempDir = Files.createTempDir();
        try {
            Double defaultFPSToExtract = 1.0;
            if (videoDuration != null && videoDuration <= ArtifactThumbnailRepository.FRAMES_PER_PREVIEW) {
                defaultFPSToExtract = (double)ArtifactThumbnailRepository.FRAMES_PER_PREVIEW / videoDuration;
            }
            extractFrames(data.getLocalFile(), tempDir, data, defaultFPSToExtract, videoRotation);

            List<String> propertyKeys = new ArrayList<>();
            for (File frameFile : tempDir.listFiles()) {
                Matcher m = fileNamePattern.matcher(frameFile.getName());
                if (!m.matches()) {
                    continue;
                }
                long frameStartTime = (long) ((Double.parseDouble(m.group(1)) / defaultFPSToExtract) * 1000.0);

                try (InputStream frameFileIn = new FileInputStream(frameFile)) {
                    ExistingElementMutation<Vertex> mutation = data.getElement().prepareMutation();
                    StreamingPropertyValue frameValue = new StreamingPropertyValue(frameFileIn, byte[].class);
                    frameValue.searchIndex(false);
                    String key = String.format("%08d", Math.max(0L, frameStartTime));
                    Metadata metadata = data.createPropertyMetadata();
                    metadata.add(VisalloProperties.MIME_TYPE.getPropertyName(), "image/png", getVisibilityTranslator().getDefaultVisibility());
                    metadata.add(MediaVisalloProperties.METADATA_VIDEO_FRAME_START_TIME, frameStartTime, getVisibilityTranslator().getDefaultVisibility());

                    MediaVisalloProperties.VIDEO_FRAME.addPropertyValue(mutation, key, frameValue, metadata, newVisibility);
                    propertyKeys.add(key);
                    mutation.save(getAuthorizations());
                }
            }

            getGraph().flush();

            generateAndSaveVideoPreviewImage((Vertex) data.getElement(), videoRotation);

            for (String propertyKey : propertyKeys) {
                getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), propertyKey, MediaVisalloProperties.VIDEO_FRAME.getPropertyName(), data.getPriority());
            }
        } finally {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    private void extractFrames(File videoFileName, File outDir, GraphPropertyWorkData data, double framesPerSecondToExtract, int videoRotation) throws IOException, InterruptedException {
        String[] ffmpegOptionsArray = prepareFFMPEGOptions(videoFileName, outDir, data, framesPerSecondToExtract, videoRotation);
        processRunner.execute(
                "ffmpeg",
                ffmpegOptionsArray,
                null,
                videoFileName.getAbsolutePath() + ": "
        );
    }

    private String[] prepareFFMPEGOptions(File videoFileName, File outDir, GraphPropertyWorkData data, double framesPerSecondToExtract, int videoRotation) {

        ArrayList<String> ffmpegOptionsList = new ArrayList<>();
        ffmpegOptionsList.add("-i");
        ffmpegOptionsList.add(videoFileName.getAbsolutePath());
        ffmpegOptionsList.add("-r");
        ffmpegOptionsList.add("" + framesPerSecondToExtract);

        String[] ffmpegVideoFilterOptions = FFprobeVideoFiltersUtil.getFFmpegVideoFilterOptions(videoRotation);
        if (ffmpegVideoFilterOptions != null) {
            ffmpegOptionsList.add(ffmpegVideoFilterOptions[0]);
            ffmpegOptionsList.add(ffmpegVideoFilterOptions[1]);
        }

        ffmpegOptionsList.add(new File(outDir, "image-%8d.png").getAbsolutePath());
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

    private void generateAndSaveVideoPreviewImage(Vertex artifactVertex, int videoRotation) {
        LOGGER.info("Generating video preview for %s", artifactVertex.getId());

        try {
            Iterable<Property> videoFrames = getVideoFrameProperties(artifactVertex);
            List<Property> videoFramesForPreview = getFramesForPreview(videoFrames);
            BufferedImage previewImage = createPreviewImage(videoFramesForPreview, videoRotation);
            saveImage(artifactVertex, previewImage);
        } catch (IOException e) {
            throw new RuntimeException("Could not create preview image for artifact: " + artifactVertex.getId(), e);
        }

        LOGGER.debug("Finished creating preview for: %s", artifactVertex.getId());
    }

    private void saveImage(Vertex artifactVertex, BufferedImage previewImage) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(previewImage, "png", out);
        StreamingPropertyValue spv = new StreamingPropertyValue(new ByteArrayInputStream(out.toByteArray()), byte[].class);
        spv.searchIndex(false);
        MediaVisalloProperties.VIDEO_PREVIEW_IMAGE.setProperty(artifactVertex, spv, artifactVertex.getVisibility(), getAuthorizations());
        getGraph().flush();
    }

    private BufferedImage createPreviewImage(List<Property> videoFrames, int videoRotation) throws IOException {
        int previewFrameWidth;
        int previewFrameHeight;
        if (videoRotation == 0 || videoRotation == 180) {
            previewFrameWidth = ArtifactThumbnailRepository.PREVIEW_FRAME_WIDTH;
            previewFrameHeight = ArtifactThumbnailRepository.PREVIEW_FRAME_HEIGHT;
        } else {
            previewFrameWidth = ArtifactThumbnailRepository.PREVIEW_FRAME_HEIGHT;
            previewFrameHeight = ArtifactThumbnailRepository.PREVIEW_FRAME_WIDTH;
        }

        BufferedImage previewImage = null;
        Graphics g = null;
        for (int i = 0; i < videoFrames.size(); i++) {
            Property videoFrame = videoFrames.get(i);
            Image img = loadImage(videoFrame);
            int widthImage = img.getWidth(null);
            int heightImage = img.getHeight(null);
            if (i == 0) {
                float ratioImage = (float)widthImage / (float)heightImage;
                float ratioContainer = (float)previewFrameWidth / (float)previewFrameHeight;
                float calculatedWidth, calculatedHeight;
                if (ratioContainer > ratioImage) {
                    calculatedWidth = widthImage * ((float)previewFrameHeight / heightImage);
                    calculatedHeight = (float)previewFrameHeight;
                } else {
                    calculatedWidth = (float)previewFrameWidth;
                    calculatedHeight = heightImage * ((float)previewFrameWidth / widthImage);
                }
                previewFrameWidth = (int) calculatedWidth;
                previewFrameHeight = (int) calculatedHeight;
                previewImage = new BufferedImage(previewFrameWidth * videoFrames.size(), previewFrameHeight, BufferedImage.TYPE_INT_RGB);
                g = previewImage.getGraphics();
            }
            int dx1 = i * previewFrameWidth;
            int dy1 = 0;
            int dx2 = dx1 + previewFrameWidth;
            int dy2 = previewFrameHeight;
            int sx1 = 0;
            int sy1 = 0;
            g.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, widthImage, heightImage, null);
        }
        return previewImage;
    }

    private Image loadImage(Property videoFrame) throws IOException {
        StreamingPropertyValue spv = (StreamingPropertyValue) videoFrame.getValue();
        try (InputStream spvIn = spv.getInputStream()) {
            BufferedImage img = ImageIO.read(spvIn);
            checkNotNull(img, "Could not load image from frame: " + videoFrame);
            return img;
        }
    }

    private Iterable<Property> getVideoFrameProperties(Vertex artifactVertex) {
        List<Property> videoFrameProperties = toList(artifactVertex.getProperties(MediaVisalloProperties.VIDEO_FRAME.getPropertyName()));
        Collections.sort(videoFrameProperties, new Comparator<Property>() {
            @Override
            public int compare(Property p1, Property p2) {
                Long p1StartTime = (Long) p1.getMetadata().getValue(MediaVisalloProperties.METADATA_VIDEO_FRAME_START_TIME);
                Long p2StartTime = (Long) p2.getMetadata().getValue(MediaVisalloProperties.METADATA_VIDEO_FRAME_START_TIME);
                return p1StartTime.compareTo(p2StartTime);
            }
        });
        return videoFrameProperties;
    }

    private List<Property> getFramesForPreview(Iterable<Property> videoFramesIterable) {
        List<Property> videoFrames = toList(videoFramesIterable);
        ArrayList<Property> results = new ArrayList<>();
        double skip = (double) videoFrames.size() / (double) ArtifactThumbnailRepository.FRAMES_PER_PREVIEW;
        for (double i = 0; i < videoFrames.size(); i += skip) {
            results.add(videoFrames.get((int) Math.floor(i)));
        }
        if (results.size() < ArtifactThumbnailRepository.FRAMES_PER_PREVIEW) {
            results.add(videoFrames.get(videoFrames.size() - 1));
        }
        if (results.size() > ArtifactThumbnailRepository.FRAMES_PER_PREVIEW) {
            results.remove(results.size() - 1);
        }
        return results;
    }

    @Inject
    public void setProcessRunner(ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }
}
