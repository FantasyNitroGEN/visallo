package org.visallo.core.model.audit;

import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.RowKeyHelper;
import org.json.JSONException;
import org.json.JSONObject;
import com.v5analytics.simpleorm.Field;

import java.util.Date;

public class AuditProperty extends Audit {
    @Field
    private String previousValue;

    @Field
    private String newValue;

    @Field
    private String propertyKey;

    @Field
    private String propertyName;

    @Field
    private JSONObject propertyMetadata;

    // Used by SimpleOrm to create instance
    @SuppressWarnings("UnusedDeclaration")
    protected AuditProperty() {
    }

    public AuditProperty(String elementId, String propertyKey, String propertyName) {
        super(RowKeyHelper.build(elementId, getDateFormat().format(new Date())), OntologyRepository.TYPE_PROPERTY);
        this.propertyKey = propertyKey;
        this.propertyName = propertyName;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(String previousValue) {
        this.previousValue = previousValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public JSONObject getPropertyMetadata() {
        return propertyMetadata;
    }

    public void setPropertyMetadata(JSONObject propertyMetadata) {
        this.propertyMetadata = propertyMetadata;
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = super.toJson();

            JSONObject propertyJson = new JSONObject();
            propertyJson.put("propertyKey", this.getPropertyKey());
            propertyJson.put("propertyName", this.getPropertyName());
            propertyJson.put("previousValue", this.getPreviousValue());
            propertyJson.put("newValue", this.getNewValue());
            if (this.getPropertyMetadata() != null) {
                propertyJson.put("propertyMetadata", this.getPropertyMetadata());
            }
            json.put("propertyAudit", propertyJson);

            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
