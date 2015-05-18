package org.visallo.twitter;

import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.vertexium.Element;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.VertexBuilder;
import org.vertexium.property.StreamingPropertyValue;

import java.io.InputStream;
import java.net.URL;

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

            VertexBuilder v = getGraph().prepareVertex(profileImageId, data.getVisibility());
            VisalloProperties.TITLE.addPropertyValue(v, MULTI_VALUE_KEY, "Profile Image of " + userTitle, data.getVisibility());
            VisalloProperties.RAW.setProperty(v, imageValue, data.getVisibility());
            VisalloProperties.CONCEPT_TYPE.setProperty(v, TwitterOntology.CONCEPT_TYPE_PROFILE_IMAGE, data.getVisibility());
            profileImageVertex = v.save(getAuthorizations());
            LOGGER.debug("created vertex: %s", profileImageVertex.getId());

            getGraph().addEdge((Vertex) data.getElement(), profileImageVertex, entityHasImageIri, data.getVisibility(), getAuthorizations());
            VisalloProperties.ENTITY_IMAGE_VERTEX_ID.setProperty(data.getElement(), profileImageVertex.getId(), data.getVisibility(), getAuthorizations());
            getGraph().flush();
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
