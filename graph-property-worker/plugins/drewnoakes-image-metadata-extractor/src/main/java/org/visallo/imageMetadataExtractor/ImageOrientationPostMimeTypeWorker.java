package org.visallo.imageMetadataExtractor;

import com.google.inject.Inject;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.ingest.graphProperty.PostMimeTypeWorker;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.ImageTransform;
import org.visallo.core.util.ImageTransformExtractor;
import org.vertexium.Authorizations;
import org.vertexium.Metadata;
import org.vertexium.Vertex;
import org.vertexium.mutation.ExistingElementMutation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Name("Drewnoakes Image Metadata")
@Description("Extracts image metadata using Drewnoakes after MIME type")
public class ImageOrientationPostMimeTypeWorker extends PostMimeTypeWorker {
    private static final String MULTI_VALUE_PROPERTY_KEY = ImageOrientationPostMimeTypeWorker.class.getName();
    private OntologyRepository ontologyRepository;
    private String yAxisFlippedIri;
    private String clockwiseRotationIri;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        yAxisFlippedIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.yAxisFlipped");
        clockwiseRotationIri = ontologyRepository.getRequiredPropertyIRIByIntent("media.clockwiseRotation");
    }

    private void setProperty(String iri, Object value, ExistingElementMutation<Vertex> mutation, Metadata metadata,
                             GraphPropertyWorkData data, List<String> properties) {
        if (iri != null && value != null) {
            mutation.setProperty(iri, value, metadata, data.getVisibility());
            properties.add(iri);
        }
    }

    @Override
    public void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception {
        if (!mimeType.startsWith("image")) {
            return;
        }

        File localFile = getLocalFileForRaw(data.getElement());
        Metadata metadata = data.createPropertyMetadata();
        ExistingElementMutation<Vertex> mutation = data.getElement().prepareMutation();
        ArrayList<String> properties = new ArrayList<>();

        ImageTransform imageTransform = ImageTransformExtractor.getImageTransform(localFile);
        setProperty(yAxisFlippedIri, imageTransform.isYAxisFlipNeeded(), mutation, metadata, data, properties);
        setProperty(clockwiseRotationIri, imageTransform.getCWRotationNeeded(), mutation, metadata, data, properties);

        mutation.save(authorizations);
        getGraph().flush();
        for (String propertyName : properties) {
            getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), MULTI_VALUE_PROPERTY_KEY, propertyName,
                    data.getPriority());
        }
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }
}
