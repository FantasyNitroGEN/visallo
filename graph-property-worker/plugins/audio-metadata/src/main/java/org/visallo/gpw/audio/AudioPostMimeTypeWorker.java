package org.visallo.gpw.audio;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Vertex;
import org.vertexium.mutation.ExistingElementMutation;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.ingest.graphProperty.PostMimeTypeWorker;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.types.DoubleVisalloProperty;
import org.visallo.core.model.properties.types.IntegerVisalloProperty;
import org.visallo.core.model.properties.types.PropertyMetadata;
import org.visallo.core.model.properties.types.VisalloPropertyUpdate;
import org.visallo.core.util.FFprobeDurationUtil;
import org.visallo.core.util.FFprobeExecutor;
import org.visallo.core.util.FileSizeUtil;
import org.visallo.core.util.ProcessRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Name("Audio Metadata")
@Description("Extracts audio metadata using FFProbe")
public class AudioPostMimeTypeWorker extends PostMimeTypeWorker {
    public static final String MULTI_VALUE_PROPERTY_KEY = AudioPostMimeTypeWorker.class.getName();
    private ProcessRunner processRunner;
    private OntologyRepository ontologyRepository;
    private DoubleVisalloProperty duration;
    private IntegerVisalloProperty fileSize;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        duration = new DoubleVisalloProperty(ontologyRepository.getRequiredPropertyIRIByIntent("media.duration"));
        fileSize = new IntegerVisalloProperty(ontologyRepository.getRequiredPropertyIRIByIntent("media.fileSize"));
    }

    @Override
    public void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception {
        if (!mimeType.startsWith("audio")) {
            return;
        }

        File localFile = getLocalFileForRaw(data.getElement());
        JSONObject audioMetadata = FFprobeExecutor.getJson(processRunner, localFile.getAbsolutePath());
        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        List<VisalloPropertyUpdate> changedProperties = new ArrayList<>();
        PropertyMetadata metadata = new PropertyMetadata(getUser(), data.getVisibilityJson(), data.getVisibility());
        if (audioMetadata != null) {
            duration.updateProperty(changedProperties, data.getElement(), m, MULTI_VALUE_PROPERTY_KEY,
                    FFprobeDurationUtil.getDuration(audioMetadata), metadata, data.getVisibility());
        }

        fileSize.updateProperty(changedProperties, data.getElement(), m, MULTI_VALUE_PROPERTY_KEY,
                FileSizeUtil.getSize(localFile), metadata, data.getVisibility());

        m.save(authorizations);
        getGraph().flush();
        getWorkQueueRepository().pushGraphVisalloPropertyQueue(data.getElement(), changedProperties, data.getPriority());
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
