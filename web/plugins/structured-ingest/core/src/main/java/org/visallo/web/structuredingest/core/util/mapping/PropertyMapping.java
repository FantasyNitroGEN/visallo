package org.visallo.web.structuredingest.core.util.mapping;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.vertexium.Visibility;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.web.clientapi.model.PropertyType;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.web.structuredingest.core.model.ClientApiMappingErrors;

import java.util.Map;

public class PropertyMapping {
    public static final String PROPERTY_MAPPING_NAME_KEY = "name";
    public static final String PROPERTY_MAPPING_VALUE_KEY = "value";
    public static final String PROPERTY_MAPPING_KEY_KEY = "key";
    public static final String PROPERTY_MAPPING_IS_IDENTIFIER_KEY = "isIdentifier";
    public static final String PROPERTY_MAPPING_ERROR_STRATEGY_KEY = "errorStrategy";
    public static final String PROPERTY_MAPPING_VISIBILITY_KEY = "visibilitySource";


    public enum ErrorHandlingStrategy {
        SET_CELL_ERROR_PROPERTY,
        SKIP_CELL,
        SKIP_VERTEX,
        SKIP_ROW
    }
    public String key;
    public String name;
    public String value;
    public boolean identifier;
    public ErrorHandlingStrategy errorHandlingStrategy;
    public VisibilityJson visibilityJson;
    public Visibility visibility;

    public PropertyMapping(VisibilityTranslator visibilityTranslator, String workspaceId, JSONObject propertyMapping) {
        this.name = propertyMapping.getString(PROPERTY_MAPPING_NAME_KEY);
        this.identifier = propertyMapping.optBoolean(PROPERTY_MAPPING_IS_IDENTIFIER_KEY, false);

        String visibilitySource = propertyMapping.optString(PROPERTY_MAPPING_VISIBILITY_KEY);
        if(!StringUtils.isBlank(visibilitySource)) {
            visibilityJson = new VisibilityJson(visibilitySource);
            visibilityJson.addWorkspace(workspaceId);
            visibility = visibilityTranslator.toVisibility(visibilityJson).getVisibility();
        }

        if(propertyMapping.has(PROPERTY_MAPPING_ERROR_STRATEGY_KEY)) {
            this.errorHandlingStrategy = ErrorHandlingStrategy.valueOf(propertyMapping.getString(PROPERTY_MAPPING_ERROR_STRATEGY_KEY));
        }

        this.value = propertyMapping.optString(PROPERTY_MAPPING_VALUE_KEY);
        this.key = propertyMapping.optString(PROPERTY_MAPPING_KEY_KEY);
    }

    public Object extractRawValue(Map<String, Object> row) {
        if(!StringUtils.isBlank(value)) {
            return value;
        } else {
            return row.get(this.key);
        }
    }

    public Object decodeValue(Map<String, Object> row) throws Exception {
        return decodeValue(extractRawValue(row));
    }

    public Object decodeValue(Object rawPropertyValue) throws Exception {
        if (rawPropertyValue instanceof String) {
            return StringUtils.isBlank((String)rawPropertyValue) ? null : rawPropertyValue;
        }
        return rawPropertyValue;
    }

    public static PropertyMapping fromJSON(OntologyRepository ontologyRepository, VisibilityTranslator visibilityTranslator, String workspaceId, JSONObject propertyMapping) {
        String propertyName = propertyMapping.getString(PROPERTY_MAPPING_NAME_KEY);
        OntologyProperty ontologyProperty = ontologyRepository.getPropertyByIRI(propertyName);
        if (ontologyProperty == null) {
            throw new VisalloException("Property " + propertyName + " was not found in the ontology.");
        }

        if (ontologyProperty.getDataType() == PropertyType.DATE) {
            return new DatePropertyMapping(visibilityTranslator, workspaceId, propertyMapping);
        } else if (ontologyProperty.getDataType() == PropertyType.BOOLEAN) {
            return new BooleanPropertyMapping(visibilityTranslator, workspaceId, propertyMapping);
        } else if (ontologyProperty.getDataType() == PropertyType.GEO_LOCATION) {
            return new GeoPointPropertyMapping(visibilityTranslator, workspaceId, propertyMapping);
        } else if (ontologyProperty.getDataType() == PropertyType.CURRENCY ||
                ontologyProperty.getDataType() == PropertyType.DOUBLE ||
                ontologyProperty.getDataType() == PropertyType.INTEGER) {
            return new NumericPropertyMapping(ontologyProperty, visibilityTranslator, workspaceId, propertyMapping);
        }
        return new PropertyMapping(visibilityTranslator, workspaceId, propertyMapping);
    }

    public ClientApiMappingErrors validate(Authorizations authorizations) {
        ClientApiMappingErrors errors = new ClientApiMappingErrors();

        if(visibility != null && !authorizations.canRead(visibility)) {
            ClientApiMappingErrors.MappingError mappingError = new ClientApiMappingErrors.MappingError();
            mappingError.propertyMapping = this;
            mappingError.attribute = PROPERTY_MAPPING_VISIBILITY_KEY;
            mappingError.message = "Invalid visibility specified.";
            errors.mappingErrors.add(mappingError);
        }

        return errors;
    }
}
