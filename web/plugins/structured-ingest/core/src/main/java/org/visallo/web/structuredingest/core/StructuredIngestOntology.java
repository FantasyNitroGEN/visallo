package org.visallo.web.structuredingest.core;

import org.visallo.core.model.properties.types.StringVisalloProperty;

public class StructuredIngestOntology {
    public static final String IRI = "http://visallo.org/structured-file";
    public static final String ELEMENT_HAS_SOURCE_IRI = IRI + "#elementHasSource";

    public static final StringVisalloProperty ERROR_MESSAGE_PROPERTY = new StringVisalloProperty(IRI + "#errorMessage");
    public static final StringVisalloProperty TARGET_PROPERTY = new StringVisalloProperty(IRI + "#targetPropertyName");
    public static final StringVisalloProperty RAW_CELL_VALUE_PROPERTY = new StringVisalloProperty(IRI + "#rawCellValue");
    public static final StringVisalloProperty SHEET_PROPERTY = new StringVisalloProperty(IRI + "#sheet");
    public static final StringVisalloProperty ROW_PROPERTY = new StringVisalloProperty(IRI + "#row");
}
