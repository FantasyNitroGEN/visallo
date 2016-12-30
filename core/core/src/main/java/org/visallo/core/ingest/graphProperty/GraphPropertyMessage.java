package org.visallo.core.ingest.graphProperty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.workQueue.Priority;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class GraphPropertyMessage {
    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, true);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    private String workspaceId;
    private String visibilitySource;
    private Priority priority;
    private boolean traceEnabled;
    private Property[] properties;
    private String[] graphVertexId;
    private String[] graphEdgeId;
    private String propertyKey;
    private String propertyName;
    private ElementOrPropertyStatus status;
    private Long beforeActionTimestamp;

    public String getWorkspaceId() {
        return workspaceId;
    }

    public GraphPropertyMessage setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
        return this;
    }

    public String getVisibilitySource() {
        return visibilitySource;
    }

    public GraphPropertyMessage setVisibilitySource(String visibilitySource) {
        this.visibilitySource = visibilitySource;
        return this;
    }

    public Priority getPriority() {
        return priority;
    }

    public GraphPropertyMessage setPriority(Priority priority) {
        checkNotNull(priority, "priority cannot be null");
        this.priority = priority;
        return this;
    }

    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public GraphPropertyMessage setPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
        return this;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public GraphPropertyMessage setPropertyName(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    public String[] getGraphEdgeId() {
        return graphEdgeId;
    }

    public GraphPropertyMessage setGraphEdgeId(String[] graphEdgeId) {
        this.graphEdgeId = graphEdgeId;
        return this;
    }

    public String[] getGraphVertexId() {
        return graphVertexId;
    }

    public GraphPropertyMessage setGraphVertexId(String[] graphVertexId) {
        this.graphVertexId = graphVertexId;
        return this;
    }

    public ElementOrPropertyStatus getStatus() {
        return status;
    }

    public GraphPropertyMessage setStatus(ElementOrPropertyStatus status) {
        this.status = status;
        return this;
    }

    public Long getBeforeActionTimestamp() {
        return beforeActionTimestamp;
    }

    @JsonIgnore
    public long getBeforeActionTimestampOrDefault() {
        return getBeforeActionTimestamp() == null ? -1L : getBeforeActionTimestamp();
    }

    public GraphPropertyMessage setBeforeActionTimestamp(Long beforeActionTimestamp) {
        this.beforeActionTimestamp = beforeActionTimestamp;
        return this;
    }

    public GraphPropertyMessage.Property[] getProperties() {
        return properties;
    }

    public GraphPropertyMessage setProperties(Property[] properties) {
        this.properties = properties;
        return this;
    }

    public static GraphPropertyMessage create(byte[] data) {
        try {
            GraphPropertyMessage message = mapper.readValue(data, GraphPropertyMessage.class);
            checkNotNull(message.getPriority(), "priority cannot be null");
            return message;
        } catch (IOException e) {
            throw new VisalloException("Could not create " + GraphPropertyMessage.class.getName() + " from " + new String(data), e);
        }
    }

    public String toJsonString() {
        try {
            checkNotNull(getPriority(), "priority cannot be null");
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new VisalloException("Could not write " + this.getClass().getName(), e);
        }
    }

    public byte[] toBytes() {
        try {
            checkNotNull(getPriority(), "priority cannot be null");
            return mapper.writeValueAsBytes(this);
        } catch (JsonProcessingException e) {
            throw new VisalloException("Could not write " + this.getClass().getName(), e);
        }
    }

    public static class Property {
        private String propertyKey;
        private String propertyName;
        private ElementOrPropertyStatus status;
        private Long beforeActionTimestamp;

        public String getPropertyKey() {
            return propertyKey;
        }

        public Property setPropertyKey(String propertyKey) {
            this.propertyKey = propertyKey;
            return this;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Property setPropertyName(String propertyName) {
            this.propertyName = propertyName;
            return this;
        }

        public ElementOrPropertyStatus getStatus() {
            return status;
        }

        public Property setStatus(ElementOrPropertyStatus status) {
            this.status = status;
            return this;
        }

        public Long getBeforeActionTimestamp() {
            return beforeActionTimestamp;
        }

        @JsonIgnore
        public long getBeforeActionTimestampOrDefault() {
            return getBeforeActionTimestamp() == null ? -1L : getBeforeActionTimestamp();
        }

        public Property setBeforeActionTimestamp(Long beforeActionTimestamp) {
            this.beforeActionTimestamp = beforeActionTimestamp;
            return this;
        }
    }
}
