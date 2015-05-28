package org.visallo.imageMetadataExtractor;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.MediaVisalloProperties;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.core.util.FileSizeUtil;
import org.vertexium.Element;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.mutation.ExistingElementMutation;
import org.visallo.imageMetadataHelper.*;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


@Name("Drewnoakes Image Metadata")
@Description("Extracts image metadata using Drewnoakes open source tool")
public class ImageMetadataGraphPropertyWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ImageMetadataGraphPropertyWorker.class);
    public static final String MULTI_VALUE_KEY = ImageMetadataGraphPropertyWorker.class.getName();
    private String fileSizeIri;
    private String dateTakenIri;
    private String deviceMakeIri;
    private String deviceModelIri;
    private String geoLocationIri;
    private String headingIri;
    private String metadataIri;
    private String widthIri;
    private String heightIri;

    @Override
    public boolean isLocalFileRequired() {
        return true;
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        headingIri = getOntologyRepository().getRequiredPropertyIRIByIntent("media.imageHeading");
        geoLocationIri = getOntologyRepository().getRequiredPropertyIRIByIntent("geoLocation");
        dateTakenIri = getOntologyRepository().getRequiredPropertyIRIByIntent("media.dateTaken");
        deviceMakeIri = getOntologyRepository().getRequiredPropertyIRIByIntent("media.deviceMake");
        deviceModelIri = getOntologyRepository().getRequiredPropertyIRIByIntent("media.deviceModel");
        widthIri = getOntologyRepository().getRequiredPropertyIRIByIntent("media.width");
        heightIri = getOntologyRepository().getRequiredPropertyIRIByIntent("media.height");
        metadataIri = getOntologyRepository().getRequiredPropertyIRIByIntent("media.metadata");
        fileSizeIri = getOntologyRepository().getRequiredPropertyIRIByIntent("media.fileSize");
    }

    private void setProperty(String iri, Object value, ExistingElementMutation<Vertex> mutation, org.vertexium.Metadata metadata, GraphPropertyWorkData data, List<String> properties) {
        if (iri != null && value != null) {
            mutation.addPropertyValue(MULTI_VALUE_KEY, iri, value, metadata, data.getVisibility());
            properties.add(iri);
        }
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        org.vertexium.Metadata metadata = data.createPropertyMetadata();
        ExistingElementMutation<Vertex> mutation = data.getElement().prepareMutation();
        List<String> properties = new ArrayList<>();

        File imageFile = data.getLocalFile();
        if (imageFile != null) {
            Metadata imageMetadata = null;
            try {
                imageMetadata = ImageMetadataReader.readMetadata(imageFile);
            } catch (Exception e) {
                LOGGER.error("Could not read metadata from imageFile: %s", imageFile, e);
            }

            if (imageMetadata != null) {
                setProperty(dateTakenIri, DateExtractor.getDateDefault(imageMetadata), mutation, metadata, data, properties);
                setProperty(deviceMakeIri, MakeExtractor.getMake(imageMetadata), mutation, metadata, data, properties);
                setProperty(deviceModelIri, ModelExtractor.getModel(imageMetadata), mutation, metadata, data, properties);
                setProperty(geoLocationIri, GeoPointExtractor.getGeoPoint(imageMetadata), mutation, metadata, data, properties);
                setProperty(headingIri, HeadingExtractor.getImageHeading(imageMetadata), mutation, metadata, data, properties);
                setProperty(metadataIri, LeftoverMetadataExtractor.getAsJSON(imageMetadata).toString(), mutation, metadata, data, properties);
            }

            Integer width = imageMetadata != null ? DimensionsExtractor.getWidthViaMetadata(imageMetadata) : DimensionsExtractor.getWidthViaBufferedImage(imageFile);
            setProperty(widthIri, width, mutation, metadata, data, properties);

            Integer height = imageMetadata != null ? DimensionsExtractor.getHeightViaMetadata(imageMetadata) : DimensionsExtractor.getHeightViaBufferedImage(imageFile);
            setProperty(heightIri, height, mutation, metadata, data, properties);

            setProperty(fileSizeIri, FileSizeUtil.getSize(imageFile), mutation, metadata, data, properties);
        }

        mutation.save(getAuthorizations());
        getGraph().flush();
        for (String propertyName : properties) {
            getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), MULTI_VALUE_KEY, propertyName, data.getPriority());
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (property.getName().equals(MediaVisalloProperties.VIDEO_FRAME.getPropertyName())) {
            return false;
        }

        String mimeType = VisalloProperties.MIME_TYPE_METADATA.getMetadataValue(property.getMetadata(), null);
        if (mimeType != null && (
                mimeType.startsWith("image/png") ||
                        mimeType.startsWith("image/jpeg") ||
                        mimeType.startsWith("image/tiff"))) {
            return true;
        }

        return false;
    }
}
