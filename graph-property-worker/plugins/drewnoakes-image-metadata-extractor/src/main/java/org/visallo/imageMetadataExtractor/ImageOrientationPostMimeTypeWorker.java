package org.visallo.imageMetadataExtractor;

import com.google.inject.Inject;
import org.vertexium.Authorizations;
import org.vertexium.Vertex;
import org.vertexium.mutation.ExistingElementMutation;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.ingest.graphProperty.PostMimeTypeWorker;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.types.BooleanSingleValueVisalloProperty;
import org.visallo.core.model.properties.types.IntegerSingleValueVisalloProperty;
import org.visallo.core.model.properties.types.PropertyMetadata;
import org.visallo.core.model.properties.types.VisalloPropertyUpdate;
import org.visallo.core.util.ImageTransform;
import org.visallo.core.util.ImageTransformExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Name("Drewnoakes Image Metadata")
@Description("Extracts image metadata using Drewnoakes after MIME type")
public class ImageOrientationPostMimeTypeWorker extends PostMimeTypeWorker {
    private OntologyRepository ontologyRepository;
    private BooleanSingleValueVisalloProperty yAxisFlippedProperty;
    private IntegerSingleValueVisalloProperty clockwiseRotationProperty;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        yAxisFlippedProperty = new BooleanSingleValueVisalloProperty(ontologyRepository.getRequiredPropertyIRIByIntent("media.yAxisFlipped"));
        clockwiseRotationProperty = new IntegerSingleValueVisalloProperty(ontologyRepository.getRequiredPropertyIRIByIntent("media.clockwiseRotation"));
    }

    @Override
    public void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception {
        if (!mimeType.startsWith("image")) {
            return;
        }

        File localFile = getLocalFileForRaw(data.getElement());
        PropertyMetadata metadata = new PropertyMetadata(getUser(), data.getVisibilityJson(), data.getVisibility());
        ExistingElementMutation<Vertex> mutation = data.getElement().prepareMutation();

        ImageTransform imageTransform = ImageTransformExtractor.getImageTransform(localFile);
        List<VisalloPropertyUpdate> changedProperties = new ArrayList<>();
        yAxisFlippedProperty.updateProperty(changedProperties, data.getElement(), mutation, imageTransform.isYAxisFlipNeeded(), metadata, data.getVisibility());
        clockwiseRotationProperty.updateProperty(changedProperties, data.getElement(), mutation, imageTransform.getCWRotationNeeded(), metadata, data.getVisibility());

        mutation.save(authorizations);
        getGraph().flush();
        getWorkQueueRepository().pushGraphVisalloPropertyQueue(data.getElement(), changedProperties, data.getPriority());
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }
}
