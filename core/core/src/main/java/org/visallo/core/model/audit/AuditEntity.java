package org.visallo.core.model.audit;

import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.RowKeyHelper;
import org.json.JSONException;
import org.json.JSONObject;
import com.v5analytics.simpleorm.Field;

import java.util.Date;

public class AuditEntity extends Audit {
    public static final String ENTITY_AUDIT = "entityAudit";

    @Field
    private String analyzedBy;

    // Used by SimpleOrm to create instance
    @SuppressWarnings("UnusedDeclaration")
    protected AuditEntity() {

    }

    public AuditEntity(String vertexId) {
        super(RowKeyHelper.build(vertexId, getDateFormat().format(new Date())), OntologyRepository.ENTITY_CONCEPT_IRI);
    }

    public String getAnalyzedBy() {
        return analyzedBy;
    }

    public AuditEntity setAnalyzedBy(String analyzedBy) {
        this.analyzedBy = analyzedBy;
        return this;
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = super.toJson();

            JSONObject entityJson = new JSONObject();
            entityJson.put("analyzedBy", this.getAnalyzedBy());
            json.put(ENTITY_AUDIT, entityJson);

            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
