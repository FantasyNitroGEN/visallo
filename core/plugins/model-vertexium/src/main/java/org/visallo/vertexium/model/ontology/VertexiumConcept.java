package org.visallo.vertexium.model.ontology;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.vertexium.Authorizations;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.mutation.ElementMutation;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.util.IterableUtils;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperties;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.util.JSONUtil;

import java.io.IOException;
import java.util.*;

public class VertexiumConcept extends Concept {
    private static Set<String> PROPERTIES_NOT_IN_METADATA = new HashSet<>();
    private final Vertex vertex;

    static {
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.DISPLAY_NAME.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.DISPLAY_TYPE.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.GLYPH_ICON.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.GLYPH_ICON_SELECTED.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.ONTOLOGY_TITLE.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(VisalloProperties.CONCEPT_TYPE.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.COLOR.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.SUBTITLE_FORMULA.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.TITLE_FORMULA.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.TIME_FORMULA.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.ADD_RELATED_CONCEPT_WHITE_LIST.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.SEARCHABLE.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.ADDABLE.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.USER_VISIBLE.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.ONTOLOGY_FILE.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.UPDATEABLE.getPropertyName());
        PROPERTIES_NOT_IN_METADATA.add(OntologyProperties.DELETEABLE.getPropertyName());
    }

    public VertexiumConcept(Vertex vertex) {
        this(vertex, null, null);
    }

    public VertexiumConcept(Vertex vertex, String parentConceptIRI, Collection<OntologyProperty> properties) {
        super(parentConceptIRI, properties);
        this.vertex = vertex;
    }

    @Override
    public String getIRI() {
        return OntologyProperties.ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    @Override
    public String getTitle() {
        return OntologyProperties.ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    @Override
    public boolean hasGlyphIconResource() {
        // TODO: This can be changed to GLYPH_ICON.getPropertyValue(vertex) once ENTITY_IMAGE_URL is added
        return vertex.getPropertyValue(OntologyProperties.GLYPH_ICON.getPropertyName()) != null;
    }

    @Override
    public boolean hasGlyphIconSelectedResource() {
        return vertex.getPropertyValue(OntologyProperties.GLYPH_ICON_SELECTED.getPropertyName()) != null;
    }

    @Override
    public String getColor() {
        return OntologyProperties.COLOR.getPropertyValue(vertex);
    }

    @Override
    public String getDisplayName() {
        return OntologyProperties.DISPLAY_NAME.getPropertyValue(vertex);
    }

    @Override
    public String getDisplayType() {
        return OntologyProperties.DISPLAY_TYPE.getPropertyValue(vertex);
    }

    @Override
    public String getTitleFormula() {
        return OntologyProperties.TITLE_FORMULA.getPropertyValue(vertex);
    }

    @Override
    public Boolean getSearchable() {
        return OntologyProperties.SEARCHABLE.getPropertyValue(vertex);
    }

    @Override
    public String getSubtitleFormula() {
        return OntologyProperties.SUBTITLE_FORMULA.getPropertyValue(vertex);
    }

    @Override
    public String getTimeFormula() {
        return OntologyProperties.TIME_FORMULA.getPropertyValue(vertex);
    }

    @Override
    public boolean getUserVisible() {
        return OntologyProperties.USER_VISIBLE.getPropertyValue(vertex, true);
    }

    @Override
    public boolean getDeleteable() {
        return OntologyProperties.DELETEABLE.getPropertyValue(vertex, true);
    }

    @Override
    public boolean getUpdateable() {
        return OntologyProperties.UPDATEABLE.getPropertyValue(vertex, true);
    }

    @Override
    public String[] getIntents() {
        return IterableUtils.toArray(OntologyProperties.INTENT.getPropertyValues(vertex), String.class);
    }

    @Override
    public void addIntent(String intent, Authorizations authorizations) {
        OntologyProperties.INTENT.addPropertyValue(vertex, intent, intent, OntologyRepository.VISIBILITY.getVisibility(), authorizations);
    }

    @Override
    public void removeIntent(String intent, Authorizations authorizations) {
        OntologyProperties.INTENT.removeProperty(vertex, intent, authorizations);
    }

    @Override
    public Map<String, String> getMetadata() {
        Map<String, String> metadata = new HashMap<>();
        for (Property p : vertex.getProperties()) {
            if (PROPERTIES_NOT_IN_METADATA.contains(p.getName())) {
                continue;
            }
            metadata.put(p.getName(), p.getValue().toString());
        }
        return metadata;
    }

    @Override
    public List<String> getAddRelatedConceptWhiteList() {
        JSONArray arr = OntologyProperties.ADD_RELATED_CONCEPT_WHITE_LIST.getPropertyValue(vertex);
        if (arr == null) {
            return null;
        }
        return JSONUtil.toStringList(arr);
    }

    @Override
    public void setProperty(String name, Object value, Authorizations authorizations) {
        getVertex().setProperty(name, value, OntologyRepository.VISIBILITY.getVisibility(), authorizations);
    }

    public void removeProperty(String key, String name, Authorizations authorizations) {
        getVertex().softDeleteProperty(key, name, authorizations);
    }

    @Override
    public void removeProperty(String name, Authorizations authorizations) {
        removeProperty(ElementMutation.DEFAULT_KEY, name, authorizations);
    }

    @Override
    public byte[] getGlyphIcon() {
        try {
            StreamingPropertyValue spv = OntologyProperties.GLYPH_ICON.getPropertyValue(getVertex());
            if (spv == null) {
                return null;
            }
            return IOUtils.toByteArray(spv.getInputStream());
        } catch (IOException e) {
            throw new VisalloResourceNotFoundException("Could not retrieve glyph icon");
        }
    }

    @Override
    public byte[] getGlyphIconSelected() {
        try {
            StreamingPropertyValue spv = OntologyProperties.GLYPH_ICON_SELECTED.getPropertyValue(getVertex());
            if (spv == null) {
                return null;
            }
            return IOUtils.toByteArray(spv.getInputStream());
        } catch (IOException e) {
            throw new VisalloResourceNotFoundException("Could not retrieve glyph icon selected");
        }
    }

    @Override
    public byte[] getMapGlyphIcon() {
        try {
            StreamingPropertyValue spv = OntologyProperties.MAP_GLYPH_ICON.getPropertyValue(getVertex());
            if (spv == null) {
                return null;
            }
            return IOUtils.toByteArray(spv.getInputStream());
        } catch (IOException e) {
            throw new VisalloResourceNotFoundException("Could not retrieve map glyph icon");
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.vertex != null ? this.vertex.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final VertexiumConcept other = (VertexiumConcept) obj;
        if (this.vertex != other.vertex && (this.vertex == null || !this.vertex.equals(other.vertex))) {
            return false;
        }
        return true;
    }

    public Vertex getVertex() {
        return this.vertex;
    }
}
