package org.visallo.core.model.audit;

import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.RowKeyHelper;
import org.json.JSONException;
import org.json.JSONObject;
import com.v5analytics.simpleorm.Field;

import java.util.Date;

public class AuditRelationship extends Audit {
    @Field
    private String sourceType;

    @Field
    private String sourceTitle;

    @Field
    private String sourceSubtype;

    @Field
    private String sourceId;

    @Field
    private String destType;

    @Field
    private String destTitle;

    @Field
    private String destId;

    @Field
    private String destSubtype;

    @Field
    private String label;

    // Used by SimpleOrm to create instance
    @SuppressWarnings("UnusedDeclaration")
    protected AuditRelationship() {

    }

    public AuditRelationship(String edgeId) {
        super(RowKeyHelper.build(edgeId, getDateFormat().format(new Date())), OntologyRepository.TYPE_RELATIONSHIP);
    }

    public String getSourceType() {
        return sourceType;
    }

    public AuditRelationship setSourceType(String sourceType) {
        this.sourceType = sourceType;
        return this;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public AuditRelationship setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
        return this;
    }

    public String getSourceSubtype() {
        return sourceSubtype;
    }

    public AuditRelationship setSourceSubtype(String sourceSubtype) {
        this.sourceSubtype = sourceSubtype;
        return this;
    }

    public String getSourceId() {
        return sourceId;
    }

    public AuditRelationship setSourceId(String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    public String getDestType() {
        return destType;
    }

    public AuditRelationship setDestType(String destType) {
        this.destType = destType;
        return this;
    }

    public String getDestTitle() {
        return destTitle;
    }

    public AuditRelationship setDestTitle(String destTitle) {
        this.destTitle = destTitle;
        return this;
    }

    public String getDestId() {
        return destId;
    }

    public AuditRelationship setDestId(String destId) {
        this.destId = destId;
        return this;
    }

    public String getDestSubtype() {
        return destSubtype;
    }

    public AuditRelationship setDestSubtype(String destSubtype) {
        this.destSubtype = destSubtype;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public AuditRelationship setLabel(String label) {
        this.label = label;
        return this;
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = super.toJson();

            JSONObject relationshipJson = new JSONObject();
            relationshipJson.put("sourceId", this.getSourceId());
            relationshipJson.put("sourceTitle", this.getSourceTitle());
            relationshipJson.put("sourceType", this.getSourceType());
            relationshipJson.put("sourceSubtype", this.getSourceSubtype());
            relationshipJson.put("destId", this.getDestId());
            relationshipJson.put("destTitle", this.getDestTitle());
            relationshipJson.put("destType", this.getDestType());
            relationshipJson.put("destSubtype", this.getDestSubtype());
            relationshipJson.put("label", this.getLabel());
            json.put("relationshipAudit", relationshipJson);

            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
