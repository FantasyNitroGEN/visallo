package org.visallo.web.structuredingest.core.util.mapping;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Visibility;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.web.structuredingest.core.model.ClientApiMappingErrors;

public class EdgeMapping {
    public static final String PROPERTY_MAPPING_VISIBILITY_KEY = "visibilitySource";
    public static final String PROPERTY_MAPPING_IN_VERTEX_KEY = "inVertex";
    public static final String PROPERTY_MAPPING_OUT_VERTEX_KEY = "outVertex";
    public static final String PROPERTY_MAPPING_LABEL_KEY = "label";

    public int inVertexIndex;
    public int outVertexIndex;
    public String label;
    public VisibilityJson visibilityJson;
    public Visibility visibility;

    public EdgeMapping(VisibilityTranslator visibilityTranslator, String workspaceId, JSONObject edgeMapping) {
        this.inVertexIndex = edgeMapping.getInt(PROPERTY_MAPPING_IN_VERTEX_KEY);
        this.outVertexIndex = edgeMapping.getInt(PROPERTY_MAPPING_OUT_VERTEX_KEY);
        this.label = edgeMapping.getString(PROPERTY_MAPPING_LABEL_KEY);

        String visibilitySource = edgeMapping.optString(PROPERTY_MAPPING_VISIBILITY_KEY);
        if(!StringUtils.isBlank(visibilitySource)) {
            visibilityJson = new VisibilityJson(visibilitySource);
            visibilityJson.addWorkspace(workspaceId);
            visibility = visibilityTranslator.toVisibility(visibilityJson).getVisibility();
        }
    }

    public ClientApiMappingErrors validate(Authorizations authorizations) {
        ClientApiMappingErrors errors = new ClientApiMappingErrors();

        if(visibility != null && !authorizations.canRead(visibility)) {
            ClientApiMappingErrors.MappingError mappingError = new ClientApiMappingErrors.MappingError();
            mappingError.edgeMapping = this;
            mappingError.attribute = PROPERTY_MAPPING_VISIBILITY_KEY;
            mappingError.message = "Invalid visibility specified.";
            errors.mappingErrors.add(mappingError);
        }

        return errors;
    }
}
