package org.visallo.core.ingest.graphProperty;

import org.vertexium.*;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.File;

public class GraphPropertyWorkData {
    private final VisibilityTranslator visibilityTranslator;
    private final Element element;
    private final Property property;
    private final String workspaceId;
    private final String visibilitySource;
    private final Priority priority;
    private File localFile;
    private ElementOrPropertyStatus status;

    public GraphPropertyWorkData(
            VisibilityTranslator visibilityTranslator,
            Element element,
            Property property,
            String workspaceId,
            String visibilitySource,
            Priority priority
    ) {
        this.visibilityTranslator = visibilityTranslator;
        this.element = element;
        this.property = property;
        this.workspaceId = workspaceId;
        this.visibilitySource = visibilitySource;
        this.priority = priority;
    }

    public GraphPropertyWorkData(
            VisibilityTranslator visibilityTranslator,
            Element element,
            Property property,
            String workspaceId,
            String visibilitySource,
            Priority priority,
            ElementOrPropertyStatus status
    ) {
        this.visibilityTranslator = visibilityTranslator;
        this.element = element;
        this.property = property;
        this.workspaceId = workspaceId;
        this.visibilitySource = visibilitySource;
        this.priority = priority;
        this.status = status;
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

    public ElementOrPropertyStatus getPropertyStatus() { return status; }

    // TODO this is a weird method. I'm not sure what this should be used for
    public VisibilityJson getVisibilitySourceJson() {
        if (getVisibilitySource() == null || getVisibilitySource().length() == 0) {
            return new VisibilityJson();
        }
        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.setSource(getVisibilitySource());
        return visibilityJson;
    }

    public VisibilityJson getVisibilityJson() {
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(getElement());
        if (visibilityJson != null) {
            return visibilityJson;
        }

        return getVisibilitySourceJson();
    }

    public Metadata createPropertyMetadata() {
        Metadata metadata = new Metadata();
        VisibilityJson visibilityJson = getVisibilityJson();
        if (visibilityJson != null) {
            VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, visibilityTranslator.getDefaultVisibility());
        }
        return metadata;
    }

    public void setVisibilityJsonOnElement(ElementBuilder builder) {
        VisibilityJson visibilityJson = getVisibilityJson();
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
                ", status=" + status +
                '}';
    }
}
