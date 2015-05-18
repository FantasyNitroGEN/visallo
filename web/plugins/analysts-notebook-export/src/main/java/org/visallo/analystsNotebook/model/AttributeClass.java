package org.visallo.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.vertexium.Vertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributeClass {
    public static final String NAME_SUBTITLE = "subtitle";
    public static final String NAME_TIME = "time";
    public static final String NAME_IMAGE_URL = "imageUrl";
    public static final String TYPE_FLAG = "AttFlag";
    public static final String TYPE_NUMBER = "AttNumber";
    public static final String TYPE_TEXT = "AttText";
    public static final String TYPE_TIME = "AttTime";

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String type;

    @JacksonXmlProperty(isAttribute = true)
    private boolean showValue;

    @JacksonXmlProperty(isAttribute = true)
    private boolean visible;

    public AttributeClass() {

    }

    public AttributeClass(String name, String type, boolean displayOnChart) {
        this.name = name;
        this.type = type;
        showValue = displayOnChart;
        visible = displayOnChart;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isShowValue() {
        return showValue;
    }

    public void setShowValue(boolean showValue) {
        this.showValue = showValue;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public static List<AttributeClass> createForVertices(Iterable<Vertex> vertices, OntologyRepository ontologyRepository) {
        Map<String, List<AttributeClass>> conceptTypeAttributeClassListMap = new HashMap<String, List<AttributeClass>>();
        for (Vertex vertex : vertices) {
            String conceptType = VisalloProperties.CONCEPT_TYPE.getPropertyValue(vertex);
            if (!conceptTypeAttributeClassListMap.containsKey(conceptType)) {
                List<AttributeClass> conceptAttributeClasses = new ArrayList<AttributeClass>();
                Concept concept = ontologyRepository.getConceptByIRI(conceptType);
                for (OntologyProperty property : concept.getProperties()) {
                    if (property.getUserVisible()) {
                        String name = property.getTitle();
                        String type = AttributeClass.TYPE_TEXT;
                        AttributeClass attributeClass = new AttributeClass(name, type, false);
                        conceptAttributeClasses.add(attributeClass);
                    }
                }
                conceptTypeAttributeClassListMap.put(conceptType, conceptAttributeClasses);
            }
        }

        List<AttributeClass> attributeClasses = new ArrayList<AttributeClass>();
        for (List<AttributeClass> conceptAttributeClasses : conceptTypeAttributeClassListMap.values()) {
            for (AttributeClass attributeClass : conceptAttributeClasses) {
                attributeClasses.add(attributeClass);
            }
        }
        return attributeClasses;
    }
}
