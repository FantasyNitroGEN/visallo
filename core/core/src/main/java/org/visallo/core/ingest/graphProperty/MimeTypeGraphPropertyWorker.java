package org.visallo.core.ingest.graphProperty;

import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.audit.AuditAction;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.vertexium.Element;
import org.vertexium.Metadata;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.mutation.ExistingElementMutation;

import java.io.InputStream;
import java.util.Collection;

public abstract class MimeTypeGraphPropertyWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(MimeTypeGraphPropertyWorker.class);
    private static final String MULTI_VALUE_KEY = MimeTypeGraphPropertyWorker.class.getName();
    private Collection<PostMimeTypeWorker> postMimeTypeWorkers;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        postMimeTypeWorkers = InjectHelper.getInjectedServices(PostMimeTypeWorker.class, getConfiguration());
        for (PostMimeTypeWorker postMimeTypeWorker : postMimeTypeWorkers) {
            try {
                postMimeTypeWorker.prepare(workerPrepareData);
            } catch (Exception ex) {
                throw new VisalloException("Could not prepare post mime type worker " + postMimeTypeWorker.getClass().getName(), ex);
            }
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(VisalloProperties.RAW.getPropertyName())) {
            return false;
        }
        if (VisalloProperties.MIME_TYPE.hasProperty(element, getMultiKey(property))) {
            return false;
        }

        return true;
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String fileName = VisalloProperties.FILE_NAME.getOnlyPropertyValue(data.getElement());
        String mimeType = getMimeType(in, fileName);
        if (mimeType == null) {
            return;
        }

        ExistingElementMutation<Vertex> m = ((Vertex) data.getElement()).prepareMutation();
        Metadata mimeTypeMetadata = data.createPropertyMetadata();
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(data.getElement());
        if (visibilityJson != null) {
            VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(mimeTypeMetadata, visibilityJson, getVisibilityTranslator().getDefaultVisibility());
        }
        VisalloProperties.MIME_TYPE.addPropertyValue(m, getMultiKey(data.getProperty()), mimeType, mimeTypeMetadata, data.getVisibility());
        m.setPropertyMetadata(data.getProperty(), VisalloProperties.MIME_TYPE.getPropertyName(), mimeType, getVisibilityTranslator().getDefaultVisibility());
        Vertex v = m.save(getAuthorizations());
        getAuditRepository().auditVertexElementMutation(AuditAction.UPDATE, m, v, this.getClass().getName(), getUser(), data.getVisibility());
        getAuditRepository().auditAnalyzedBy(AuditAction.ANALYZED_BY, v, getClass().getSimpleName(), getUser(), v.getVisibility());

        getGraph().flush();

        runPostMimeTypeWorkers(mimeType, data);

        getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), data.getProperty(), data.getWorkspaceId(), data.getVisibilitySource(), data.getPriority());
    }

    private String getMultiKey(Property property) {
        return MULTI_VALUE_KEY + property.getKey();
    }

    private void runPostMimeTypeWorkers(String mimeType, GraphPropertyWorkData data) {
        for (PostMimeTypeWorker postMimeTypeWorker : postMimeTypeWorkers) {
            try {
                LOGGER.debug("running PostMimeTypeWorker: %s on element: %s, mimeType: %s", postMimeTypeWorker.getClass().getName(), data.getElement().getId(), mimeType);
                postMimeTypeWorker.executeAndCleanup(mimeType, data, getAuthorizations());
            } catch (Exception ex) {
                throw new VisalloException("Failed running PostMimeTypeWorker " + postMimeTypeWorker.getClass().getName(), ex);
            }
        }
        if (postMimeTypeWorkers.size() > 0) {
            getGraph().flush();
        }
    }

    protected abstract String getMimeType(InputStream in, String fileName) throws Exception;
}
