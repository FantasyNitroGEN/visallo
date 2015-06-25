package org.visallo.gpw.video;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Metadata;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.vertexium.mutation.ExistingElementMutation;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.ingest.graphProperty.PostMimeTypeWorker;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Name("Video Metadata")
@Description("Extracts video metadata using FFProbe")
public class VideoPostMimeTypeWorker extends PostMimeTypeWorker {
    public static final String MULTI_VALUE_PROPERTY_KEY = VideoPostMimeTypeWorker.class.getName();
    private ProcessRunner processRunner;
    private OntologyRepository ontologyRepository;
    private String durationIri;
    private String geoLocationIri;
    private String dateTakenIri;
    private String deviceMakeIri;
    private String deviceModelIri;
    private String widthIri;
    private String heightIri;
    private String metadataIri;
    private String clockwiseRotationIri;
    private String fileSizeIri;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        durationIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.duration");
        geoLocationIri = ontologyRepository.getRequiredPropertyIRIByIntent("geoLocation");
        dateTakenIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.dateTaken");
        deviceMakeIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.deviceMake");
        deviceModelIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.deviceModel");
        widthIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.width");
        heightIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.height");
        metadataIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.metadata");
        clockwiseRotationIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.clockwiseRotation");
        fileSizeIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.fileSize");
    }

    @Override
    public void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception {
        if (!mimeType.startsWith("video")) {
            return;
        }

        File localFile = getLocalFileForRaw(data.getElement());
        JSONObject videoMetadata = FFprobeExecutor.getJson(processRunner, localFile.getAbsolutePath());
        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        List<String> properties = new ArrayList<String>();
        Metadata metadata = data.createPropertyMetadata();
        if (videoMetadata != null) {
            setProperty(durationIri, FFprobeDurationUtil.getDuration(videoMetadata), m, metadata, data, properties);
            setProperty(geoLocationIri, FFprobeGeoLocationUtil.getGeoPoint(videoMetadata), m, metadata, data, properties);
            setProperty(dateTakenIri, FFprobeDateUtil.getDateTaken(videoMetadata), m, metadata, data, properties);
            setProperty(deviceMakeIri, FFprobeMakeAndModelUtil.getMake(videoMetadata), m, metadata, data, properties);
            setProperty(deviceModelIri, FFprobeMakeAndModelUtil.getModel(videoMetadata), m, metadata, data, properties);
            setProperty(widthIri, FFprobeDimensionsUtil.getWidth(videoMetadata), m, metadata, data, properties);
            setProperty(heightIri, FFprobeDimensionsUtil.getHeight(videoMetadata), m, metadata, data, properties);
            setProperty(metadataIri, videoMetadata.toString(), m, metadata, data, properties);
            setProperty(clockwiseRotationIri, FFprobeVideoFiltersUtil.getRotation(videoMetadata), m, metadata, data, properties);
        }

        setProperty(fileSizeIri, FileSizeUtil.getSize(localFile), m, metadata, data, properties);

        m.save(authorizations);
        getGraph().flush();

        for (String propertyName : properties) {
            getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), MULTI_VALUE_PROPERTY_KEY, propertyName, data.getPriority());
        }

        getGraph().flush();
    }

    private void setProperty(String iri, Object value, ExistingElementMutation<Vertex> mutation, Metadata metadata, GraphPropertyWorkData data, List<String> properties) {
        if (iri != null && value != null) {
            mutation.addPropertyValue(MULTI_VALUE_PROPERTY_KEY, iri, value, metadata, new Visibility(data.getVisibilitySource()));
            properties.add(iri);
        }
    }

    @Inject
    public void setProcessRunner(ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }
}
