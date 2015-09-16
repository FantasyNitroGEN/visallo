package org.visallo.twitter;

import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.PropertyMetadata;
import org.visallo.core.model.properties.types.VisalloPropertyUpdate;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class TwitterProfileImageDownloadGraphPropertyWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(TwitterProfileImageDownloadGraphPropertyWorker.class);
    private static final String MULTI_VALUE_KEY = TwitterProfileImageDownloadGraphPropertyWorker.class.getName();
    private String entityHasImageIri;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        this.entityHasImageIri = getOntologyRepository().getRequiredRelationshipIRIByIntent("entityHasImage");
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String profileImageUrlString = getStringFromPropertyValue(data.getProperty().getValue());
        if (profileImageUrlString == null || profileImageUrlString.trim().length() == 0) {
            return;
        }

        String profileImageId = "TWITTER_PROFILE_IMAGE_" + profileImageUrlString;

        Vertex profileImageVertex = getGraph().getVertex(profileImageId, getAuthorizations());
        if (profileImageVertex != null) {
            return;
        }

        LOGGER.debug("downloading: %s", profileImageUrlString);
        URL profileImageUrl = new URL(profileImageUrlString);
        try (InputStream imageData = profileImageUrl.openStream()) {
            String userTitle = VisalloProperties.TITLE.getOnlyPropertyValue(data.getElement());

            StreamingPropertyValue imageValue = new StreamingPropertyValue(imageData, byte[].class);
            imageValue.searchIndex(false);

            List<VisalloPropertyUpdate> profileImagePropertyUpdates = new ArrayList<>();
            PropertyMetadata metadata = new PropertyMetadata(getUser(), data.getVisibilityJson(), data.getVisibility());
            VertexBuilder v = getGraph().prepareVertex(profileImageId, data.getVisibility());
            VisalloProperties.TITLE.updateProperty(profileImagePropertyUpdates, null, v, MULTI_VALUE_KEY, "Profile Image of " + userTitle, metadata, data.getVisibility());
            VisalloProperties.RAW.updateProperty(profileImagePropertyUpdates, null, v, imageValue, metadata, data.getVisibility());
            VisalloProperties.CONCEPT_TYPE.updateProperty(profileImagePropertyUpdates, null, v, TwitterOntology.CONCEPT_TYPE_PROFILE_IMAGE, metadata, data.getVisibility());
            profileImageVertex = v.save(getAuthorizations());
            LOGGER.debug("created vertex: %s", profileImageVertex.getId());

            List<VisalloPropertyUpdate> elementPropertyUpdates = new ArrayList<>();
            Edge entityHasImageEdge = getGraph().addEdge((Vertex) data.getElement(), profileImageVertex, entityHasImageIri, data.getVisibility(), getAuthorizations());
            ElementMutation m = ((Vertex) data.getElement()).prepareMutation();
            VisalloProperties.ENTITY_IMAGE_VERTEX_ID.updateProperty(elementPropertyUpdates, data.getElement(), m, profileImageVertex.getId(), metadata, data.getVisibility());
            m.save(getAuthorizations());

            getGraph().flush();

            getWorkQueueRepository().pushElement(entityHasImageEdge, Priority.LOW);
            getWorkQueueRepository().pushGraphVisalloPropertyQueue(profileImageVertex, profileImagePropertyUpdates, Priority.LOW);
            getWorkQueueRepository().pushGraphVisalloPropertyQueue(data.getElement(), elementPropertyUpdates, Priority.LOW);
        }
    }

    private String getStringFromPropertyValue(Object value) {
        checkNotNull(value, "property value cannot be null");
        if (value instanceof String) {
            return (String) value;
        }
        throw new ClassCastException("Could not convert " + value.getClass().getName() + " to string");
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        return property.getName().equals(TwitterOntology.PROFILE_IMAGE_URL.getPropertyName());
    }
}
