package org.visallo.opencvObjectDetector;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;
import org.vertexium.Element;
import org.vertexium.Metadata;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.visallo.core.ingest.ArtifactDetectedObject;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import org.visallo.core.model.file.FileSystemRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Name("OpenCV Object Detector")
@Description("Detects objects in images using OpenCV")
public class OpenCVObjectDetectorPropertyWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(OpenCVObjectDetectorPropertyWorker.class);
    public static final String MULTI_VALUE_KEY_PREFIX = OpenCVObjectDetectorPropertyWorker.class.getName();
    public static final String OPENCV_CLASSIFIER_CONCEPT_LIST = "objectdetection.classifierConcepts";
    public static final String OPENCV_CLASSIFIER_PATH_PREFIX = "objectdetection.classifier.";
    public static final String OPENCV_CLASSIFIER_PATH_SUFFIX = ".path";
    private static final String PROCESS = OpenCVObjectDetectorPropertyWorker.class.getName();
    private final ArtifactThumbnailRepository artifactThumbnailRepository;
    private final FileSystemRepository fileSystemRepository;

    private List<CascadeClassifierHolder> objectClassifiers = new ArrayList<>();

    public OpenCVObjectDetectorPropertyWorker(
            ArtifactThumbnailRepository artifactThumbnailRepository,
            FileSystemRepository fileSystemRepository
    ) {
        this.artifactThumbnailRepository = artifactThumbnailRepository;
        this.fileSystemRepository = fileSystemRepository;
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        loadNativeLibrary();

        String conceptListString = (String) workerPrepareData.getConfiguration().get(OPENCV_CLASSIFIER_CONCEPT_LIST);
        checkNotNull(conceptListString, OPENCV_CLASSIFIER_CONCEPT_LIST + " is a required configuration parameter");
        String[] classifierConcepts = conceptListString.split(",");
        for (String classifierConcept : classifierConcepts) {
            String classifiedConfigPath = OPENCV_CLASSIFIER_PATH_PREFIX + classifierConcept + OPENCV_CLASSIFIER_PATH_SUFFIX;
            String classifierFilePath = (String) workerPrepareData.getConfiguration().get(classifiedConfigPath);
            checkNotNull(classifierFilePath, classifiedConfigPath + " is required");
            File localFile = fileSystemRepository.getLocalFileFor(classifierFilePath);
            CascadeClassifier objectClassifier = new CascadeClassifier(localFile.getPath());
            String conceptIRI = getOntologyRepository().getRequiredConceptIRIByIntent(classifierConcept);
            addObjectClassifier(classifierConcept, objectClassifier, conceptIRI);
            if (!localFile.delete()) {
                LOGGER.warn("Could not delete file: %s", localFile.getAbsolutePath());
            }
        }
    }

    public void loadNativeLibrary() {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError ex) {
            String javaLibraryPath = System.getProperty("java.library.path");
            throw new RuntimeException("Could not find opencv library: " + Core.NATIVE_LIBRARY_NAME + " (java.library.path: " + javaLibraryPath + ")", ex);
        }
    }

    public void addObjectClassifier(String concept, CascadeClassifier objectClassifier, String conceptIRI) {
        objectClassifiers.add(new CascadeClassifierHolder(concept, objectClassifier, conceptIRI));
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        BufferedImage originalImage = ImageIO.read(in);
        Vertex artifactVertex = (Vertex) data.getElement();
        BufferedImage bImage = artifactThumbnailRepository.getTransformedImage(originalImage, artifactVertex, data.getProperty().getKey());
        List<ArtifactDetectedObject> detectedObjects = detectObjects(bImage);

        Metadata metadata = data.createPropertyMetadata();
        saveDetectedObjects((Vertex) data.getElement(), metadata, detectedObjects, data.getPriority());
    }

    private void saveDetectedObjects(Vertex artifactVertex, Metadata metadata, List<ArtifactDetectedObject> detectedObjects, Priority priority) {
        List<String> propertyKeys = new ArrayList<>();
        for (ArtifactDetectedObject detectedObject : detectedObjects) {
            propertyKeys.add(saveDetectedObject(artifactVertex, metadata, detectedObject));
        }
        getGraph().flush();
        for (String propKey : propertyKeys) {
            getWorkQueueRepository().pushGraphPropertyQueue(artifactVertex, propKey, VisalloProperties.DETECTED_OBJECT.getPropertyName(), priority);
        }
    }

    private String saveDetectedObject(Vertex artifactVertex, Metadata metadata, ArtifactDetectedObject detectedObject) {
        String multiKey = detectedObject.getMultivalueKey(MULTI_VALUE_KEY_PREFIX);
        VisalloProperties.DETECTED_OBJECT.addPropertyValue(artifactVertex, multiKey, detectedObject, metadata, artifactVertex.getVisibility(), getAuthorizations());
        return multiKey;
    }

    public List<ArtifactDetectedObject> detectObjects(BufferedImage bImage) {
        List<ArtifactDetectedObject> detectedObjectList = new ArrayList<>();
        Mat image = OpenCVUtils.bufferedImageToMat(bImage);
        if (image != null) {
            MatOfRect faceDetections = new MatOfRect();
            double width = image.width();
            double height = image.height();
            for (CascadeClassifierHolder objectClassifier : objectClassifiers) {
                objectClassifier.cascadeClassifier.detectMultiScale(image, faceDetections);

                for (Rect rect : faceDetections.toArray()) {
                    ArtifactDetectedObject detectedObject = new ArtifactDetectedObject(
                            rect.x / width,
                            rect.y / height,
                            (rect.x + rect.width) / width,
                            (rect.y + rect.height) / height,
                            objectClassifier.conceptIRI,
                            PROCESS);
                    detectedObjectList.add(detectedObject);
                }
            }
        }
        return detectedObjectList;
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        String mimeType = VisalloProperties.MIME_TYPE_METADATA.getMetadataValue(property.getMetadata(), null);
        return !(mimeType == null || !mimeType.startsWith("image"));
    }

    private class CascadeClassifierHolder {
        public final String concept;
        public final CascadeClassifier cascadeClassifier;
        public final String conceptIRI;

        public CascadeClassifierHolder(String concept, CascadeClassifier cascadeClassifier, String conceptIRI) {
            this.concept = concept;
            this.cascadeClassifier = cascadeClassifier;
            this.conceptIRI = conceptIRI;
        }
    }
}
