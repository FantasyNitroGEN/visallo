package org.visallo.gpw.audio;

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

@Name("Audio Metadata")
@Description("Extracts audio metadata using FFProbe")
public class AudioPostMimeTypeWorker extends PostMimeTypeWorker {
    public static final String MULTI_VALUE_PROPERTY_KEY = AudioPostMimeTypeWorker.class.getName();
    private ProcessRunner processRunner;
    private OntologyRepository ontologyRepository;
    private String durationIri;
    private String fileSizeIri;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        durationIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.duration");
        fileSizeIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.fileSize");
    }

    @Override
    public void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception {
        if (!mimeType.startsWith("audio")) {
            return;
        }

        File localFile = getLocalFileForRaw(data.getElement());
        JSONObject audioMetadata = FFprobeExecutor.getJson(processRunner, localFile.getAbsolutePath());
        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        List<String> properties = new ArrayList<>();
        Metadata metadata = data.createPropertyMetadata();
        if (audioMetadata != null) {
            setProperty(durationIri, FFprobeDurationUtil.getDuration(audioMetadata), m, metadata, data, properties);
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
