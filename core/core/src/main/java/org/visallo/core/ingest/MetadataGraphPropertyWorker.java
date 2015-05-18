package org.visallo.core.ingest;

import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.Element;
import org.vertexium.Metadata;
import org.vertexium.Property;
import org.vertexium.Visibility;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.property.StreamingPropertyValue;

import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

@Name("Metadata Processor")
@Description("Adds properties to a vertex from a metadata JSON document")
public class MetadataGraphPropertyWorker extends GraphPropertyWorker {
    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        JSONObject metadataJson = getMetadataJson(data);

        JSONArray propertiesJson = metadataJson.optJSONArray("properties");
        if (propertiesJson == null) {
            return;
        }

        for (int i = 0; i < propertiesJson.length(); i++) {
            JSONObject propertyJson = propertiesJson.getJSONObject(i);
            setProperty(propertyJson, data);
        }

        getGraph().flush();

        for (int i = 0; i < propertiesJson.length(); i++) {
            JSONObject propertyJson = propertiesJson.getJSONObject(i);
            queueProperty(propertyJson, data);
        }
    }

    public void queueProperty(JSONObject propertyJson, GraphPropertyWorkData data) {
        String propertyKey = propertyJson.optString("key");
        if (propertyKey == null) {
            propertyKey = ElementMutation.DEFAULT_KEY;
        }
        String propertyName = propertyJson.optString("name");
        getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), propertyKey, propertyName, data.getPriority());
    }

    private void setProperty(JSONObject propertyJson, GraphPropertyWorkData data) {
        String propertyKey = propertyJson.optString("key", null);
        if (propertyKey == null) {
            propertyKey = ElementMutation.DEFAULT_KEY;
        }

        String propertyName = propertyJson.optString("name", null);
        checkNotNull(propertyName, "name is required: " + propertyJson.toString());

        String propertyValue = propertyJson.optString("value", null);
        checkNotNull(propertyValue, "value is required: " + propertyJson.toString());

        String visibilitySource = propertyJson.optString("visibilitySource", null);
        Visibility visibility;
        if (visibilitySource == null) {
            visibility = data.getVisibility();
        } else {
            visibility = new Visibility(visibilitySource);
        }

        Metadata metadata = new Metadata();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, VisibilityJson.updateVisibilitySource(null, visibilitySource), getVisibilityTranslator().getDefaultVisibility());

        data.getElement().addPropertyValue(propertyKey, propertyName, propertyValue, metadata, visibility, getAuthorizations());
    }

    public JSONObject getMetadataJson(GraphPropertyWorkData data) throws IOException {
        StreamingPropertyValue metadataJsonValue = VisalloProperties.METADATA_JSON.getPropertyValue(data.getElement());
        try (InputStream metadataJsonIn = metadataJsonValue.getInputStream()) {
            String metadataJsonString = IOUtils.toString(metadataJsonIn);
            return new JSONObject(metadataJsonString);
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property != null) {
            return false;
        }

        StreamingPropertyValue mappingJson = VisalloProperties.METADATA_JSON.getPropertyValue(element);
        if (mappingJson == null) {
            return false;
        }

        return true;
    }
}
