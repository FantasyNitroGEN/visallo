package org.visallo.web.structuredingest.core.util.mapping;

public enum ColumnMappingType {
    Boolean,

    /**
     * Number of days from epoch
     */
    Date,

    /**
     * Number of millis from epoch
     */
    DateTime,
    GeoPoint,
    Number,
    String,
    Unknown
}
