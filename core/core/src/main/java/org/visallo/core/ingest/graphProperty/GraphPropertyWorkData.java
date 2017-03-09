package org.visallo.core.ingest.graphProperty;

import org.vertexium.*;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.File;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

public class GraphPropertyWorkData {
    private final VisibilityTranslator visibilityTranslator;
    private final Element element;
    private final Property property;
    private final String workspaceId;
    private final String visibilitySource;
    private final Priority priority;
    private final boolean traceEnabled;
    private File localFile;
    private long beforeActionTimestamp;
    private ElementOrPropertyStatus status;

    public GraphPropertyWorkData(
            VisibilityTranslator visibilityTranslator,
            Element element,
            Property property,
            String workspaceId,
            String visibilitySource,
            Priority priority,
            boolean traceEnabled
    ) {
        checkNotNull(priority, "priority cannot be null");
        this.visibilityTranslator = visibilityTranslator;
        this.element = element;
        this.property = property;
        this.workspaceId = workspaceId;
        this.visibilitySource = visibilitySource;
        this.priority = priority;
        this.traceEnabled = traceEnabled;
    }

    public GraphPropertyWorkData(
            VisibilityTranslator visibilityTranslator,
            Element element,
            Property property,
            String workspaceId,
            String visibilitySource,
            Priority priority,
            boolean traceEnabled,
            long beforeActionTimestamp,
            ElementOrPropertyStatus status
    ) {
        checkNotNull(priority, "priority cannot be null");
        this.visibilityTranslator = visibilityTranslator;
        this.element = element;
        this.property = property;
        this.workspaceId = workspaceId;
        this.visibilitySource = visibilitySource;
        this.priority = priority;
        this.beforeActionTimestamp = beforeActionTimestamp;
        this.status = status;
        this.traceEnabled = traceEnabled;
    }

    public Element getElement() {
        return element;
    }

    public Property getProperty() {
        return property;
    }

    public void setLocalFile(File localFile) {
        this.localFile = localFile;
    }

    public File getLocalFile() {
        return localFile;
    }

    public Visibility getVisibility() {
        return getElement().getVisibility();
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getVisibilitySource() {
        return visibilitySource;
    }

    public Priority getPriority() {
        return priority;
    }

    public long getBeforeActionTimestamp() {
        return beforeActionTimestamp;
    }

    public ElementOrPropertyStatus getPropertyStatus() {
        return status;
    }

    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    // TODO this is a weird method. I'm not sure what this should be used for
    public VisibilityJson getVisibilitySourceJson() {
        if (getVisibilitySource() == null || getVisibilitySource().length() == 0) {
            return new VisibilityJson();
        }
        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.setSource(getVisibilitySource());
        return visibilityJson;
    }

    /**
     * @deprecated  replaced by {@link #getElementVisibilityJson()}
     */
    @Deprecated
    public VisibilityJson getVisibilityJson() {
        return getElementVisibilityJson();
    }

    public VisibilityJson getElementVisibilityJson() {
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(getElement());
        if (visibilityJson != null) {
            return visibilityJson;
        }

        return getVisibilitySourceJson();
    }

    public VisibilityJson getPropertyVisibilityJson() {
        if (property != null) {
            VisibilityJson propertyVisibilityJson = VisalloProperties.VISIBILITY_JSON_METADATA.getMetadataValue(property);
            if (propertyVisibilityJson != null) {
                return propertyVisibilityJson;
            }
        }
        return getElementVisibilityJson();
    }

    public Metadata createPropertyMetadata(User user) {
        Metadata metadata = new Metadata();
        VisibilityJson visibilityJson = getElementVisibilityJson();
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        if (visibilityJson != null) {
            
            VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, defaultVisibility);
        }
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, new Date(), defaultVisibility);
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(metadata, user.getUserId(), defaultVisibility);
        return metadata;
    }

    public void setVisibilityJsonOnElement(ElementBuilder builder) {
        VisibilityJson visibilityJson = getElementVisibilityJson();
        if (visibilityJson != null) {
            VisalloProperties.VISIBILITY_JSON.setProperty(builder, visibilityJson, visibilityTranslator.getDefaultVisibility());
        }
    }

    public void setVisibilityJsonOnElement(Element element, Authorizations authorizations) {
        VisibilityJson visibilityJson = getVisibilitySourceJson();
        if (visibilityJson != null) {
            VisalloProperties.VISIBILITY_JSON.setProperty(element, visibilityJson, visibilityTranslator.getDefaultVisibility(), authorizations);
        }
    }

    @Override
    public String toString() {
        return "GraphPropertyWorkData{" +
                "element=" + element +
                ", property=" + property +
                ", workspaceId='" + workspaceId + '\'' +
                ", priority=" + priority +
                ", traceEnabled=" + traceEnabled +
                ", status=" + status +
                '}';
    }
}
