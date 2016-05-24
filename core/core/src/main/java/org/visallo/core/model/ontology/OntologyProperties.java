package org.visallo.core.model.ontology;

import com.google.common.collect.ImmutableSet;
import org.visallo.core.model.properties.types.*;

import java.util.Set;

public class OntologyProperties {
    public static final String EDGE_LABEL_DEPENDENT_PROPERTY = "http://visallo.org#dependentPropertyIri";

    public static final StringSingleValueVisalloProperty TITLE = new StringSingleValueVisalloProperty("http://visallo.org#title");
    public static final StreamingVisalloProperty ONTOLOGY_FILE = new StreamingVisalloProperty("http://visallo.org#ontologyFile");
    public static final IntegerSingleValueVisalloProperty DEPENDENT_PROPERTY_ORDER_PROPERTY_NAME = new IntegerSingleValueVisalloProperty("order");
    public static final StringVisalloProperty TEXT_INDEX_HINTS = new StringVisalloProperty("http://visallo.org#textIndexHints");
    public static final StringSingleValueVisalloProperty ONTOLOGY_TITLE = new StringSingleValueVisalloProperty("http://visallo.org#ontologyTitle");
    public static final StringSingleValueVisalloProperty DISPLAY_NAME = new StringSingleValueVisalloProperty("http://visallo.org#displayName");
    public static final StringSingleValueVisalloProperty DISPLAY_TYPE = new StringSingleValueVisalloProperty("http://visallo.org#displayType");
    public static final BooleanSingleValueVisalloProperty USER_VISIBLE = new BooleanSingleValueVisalloProperty("http://visallo.org#userVisible");
    public static final StringSingleValueVisalloProperty GLYPH_ICON_FILE_NAME = new StringSingleValueVisalloProperty("http://visallo.org#glyphIconFileName");
    public static final StreamingSingleValueVisalloProperty GLYPH_ICON = new StreamingSingleValueVisalloProperty("http://visallo.org#glyphIcon");
    public static final StringSingleValueVisalloProperty GLYPH_ICON_SELECTED_FILE_NAME = new StringSingleValueVisalloProperty("http://visallo.org#glyphIconSelectedFileName");
    public static final StreamingSingleValueVisalloProperty GLYPH_ICON_SELECTED = new StreamingSingleValueVisalloProperty("http://visallo.org#glyphIconSelected");
    public static final StreamingSingleValueVisalloProperty MAP_GLYPH_ICON = new StreamingSingleValueVisalloProperty("http://visallo.org#mapGlyphIcon");
    public static final StringSingleValueVisalloProperty MAP_GLYPH_ICON_FILE_NAME = new StringSingleValueVisalloProperty("http://visallo.org#mapGlyphIconFileName");
    public static final JsonArraySingleValueVisalloProperty ADD_RELATED_CONCEPT_WHITE_LIST = new JsonArraySingleValueVisalloProperty("http://visallo.org#addRelatedConceptWhiteList");
    public static final StringVisalloProperty INTENT = new StringVisalloProperty("http://visallo.org#intent");
    public static final BooleanSingleValueVisalloProperty SEARCHABLE = new BooleanSingleValueVisalloProperty("http://visallo.org#searchable");
    public static final BooleanSingleValueVisalloProperty SORTABLE = new BooleanSingleValueVisalloProperty("http://visallo.org#sortable");
    public static final BooleanSingleValueVisalloProperty ADDABLE = new BooleanSingleValueVisalloProperty("http://visallo.org#addable");
    public static final StringSingleValueVisalloProperty DISPLAY_FORMULA = new StringSingleValueVisalloProperty("http://visallo.org#displayFormula");
    public static final StringSingleValueVisalloProperty PROPERTY_GROUP = new StringSingleValueVisalloProperty("http://visallo.org#propertyGroup");
    public static final StringSingleValueVisalloProperty VALIDATION_FORMULA = new StringSingleValueVisalloProperty("http://visallo.org#validationFormula");
    public static final StringSingleValueVisalloProperty TIME_FORMULA = new StringSingleValueVisalloProperty("http://visallo.org#timeFormula");
    public static final StringSingleValueVisalloProperty TITLE_FORMULA = new StringSingleValueVisalloProperty("http://visallo.org#titleFormula");
    public static final StringSingleValueVisalloProperty SUBTITLE_FORMULA = new StringSingleValueVisalloProperty("http://visallo.org#subtitleFormula");
    public static final StringSingleValueVisalloProperty COLOR = new StringSingleValueVisalloProperty("http://visallo.org#color");
    public static final StringSingleValueVisalloProperty DATA_TYPE = new StringSingleValueVisalloProperty("http://visallo.org#dataType");
    public static final DoubleSingleValueVisalloProperty BOOST = new DoubleSingleValueVisalloProperty("http://visallo.org#boost");
    public static final JsonSingleValueVisalloProperty POSSIBLE_VALUES = new JsonSingleValueVisalloProperty("http://visallo.org#possibleValues");
    public static final BooleanSingleValueVisalloProperty DELETEABLE = new BooleanSingleValueVisalloProperty("http://visallo.org#deleteable");
    public static final BooleanSingleValueVisalloProperty UPDATEABLE = new BooleanSingleValueVisalloProperty("http://visallo.org#updateable");

    public static final Set<String> CHANGEABLE_PROPERTY_IRI = ImmutableSet.of(
            DISPLAY_TYPE.getPropertyName(),
            USER_VISIBLE.getPropertyName(),
            DELETEABLE.getPropertyName(),
            UPDATEABLE.getPropertyName(),
            ADDABLE.getPropertyName(),
            SORTABLE.getPropertyName(),
            SEARCHABLE.getPropertyName(),
            INTENT.getPropertyName(),
            POSSIBLE_VALUES.getPropertyName(),
            COLOR.getPropertyName(),
            SUBTITLE_FORMULA.getPropertyName(),
            TIME_FORMULA.getPropertyName(),
            TITLE_FORMULA.getPropertyName(),
            VALIDATION_FORMULA.getPropertyName(),
            PROPERTY_GROUP.getPropertyName(),
            DISPLAY_FORMULA.getPropertyName(),
            GLYPH_ICON_FILE_NAME.getPropertyName(),
            GLYPH_ICON.getPropertyName(),
            GLYPH_ICON_SELECTED_FILE_NAME.getPropertyName(),
            GLYPH_ICON_SELECTED.getPropertyName(),
            MAP_GLYPH_ICON.getPropertyName(),
            MAP_GLYPH_ICON_FILE_NAME.getPropertyName(),
            ADD_RELATED_CONCEPT_WHITE_LIST.getPropertyName()
    );
}