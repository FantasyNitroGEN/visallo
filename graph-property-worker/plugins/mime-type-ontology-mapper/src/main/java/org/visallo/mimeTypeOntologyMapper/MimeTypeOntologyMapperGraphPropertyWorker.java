package org.visallo.mimeTypeOntologyMapper;

import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.vertexium.Element;
import org.vertexium.Property;

import java.io.InputStream;

@Name("MIME Type Ontology Mapper")
@Description("Maps MIME types to an ontology class")
public class MimeTypeOntologyMapperGraphPropertyWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(MimeTypeOntologyMapperGraphPropertyWorker.class);
    private Concept imageConcept;
    private Concept audioConcept;
    private Concept videoConcept;
    private Concept documentConcept;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        imageConcept = getOntologyRepository().getRequiredConceptByIntent("image");
        audioConcept = getOntologyRepository().getRequiredConceptByIntent("audio");
        videoConcept = getOntologyRepository().getRequiredConceptByIntent("video");
        documentConcept = getOntologyRepository().getRequiredConceptByIntent("document");
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String mimeType = VisalloProperties.MIME_TYPE.getOnlyPropertyValue(data.getElement());
        Concept concept = null;

        if (imageConcept != null && mimeType.startsWith("image")) {
            concept = imageConcept;
        } else if (audioConcept != null && mimeType.startsWith("audio")) {
            concept = audioConcept;
        } else if (videoConcept != null && mimeType.startsWith("video")) {
            concept = videoConcept;
        } else if (documentConcept != null) {
            concept = documentConcept;
        }

        if (concept == null) {
            LOGGER.debug("skipping, no concept mapped for vertex " + data.getElement().getId());
            return;
        }

        LOGGER.debug("assigning concept type %s to vertex %s", concept.getIRI(), data.getElement().getId());
        VisalloProperties.CONCEPT_TYPE.setProperty(data.getElement(), concept.getIRI(), data.createPropertyMetadata(), data.getVisibility(), getAuthorizations());
        getGraph().flush();
        getWorkQueueRepository().pushGraphPropertyQueue(
                data.getElement(),
                null,
                VisalloProperties.CONCEPT_TYPE.getPropertyName(),
                data.getWorkspaceId(),
                data.getVisibilitySource(),
                data.getPriority()
        );
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(VisalloProperties.RAW.getPropertyName())) {
            return false;
        }

        if (!VisalloProperties.MIME_TYPE.hasProperty(element)) {
            return false;
        }

        String existingConceptType = VisalloProperties.CONCEPT_TYPE.getPropertyValue(element);
        if (existingConceptType != null) {
            return false;
        }

        return true;
    }
}
