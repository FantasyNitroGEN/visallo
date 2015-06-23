package org.visallo.core.model.properties;

import org.visallo.core.model.properties.types.*;
import org.visallo.core.model.termMention.TermMentionForProperty;

public class VisalloProperties {
    public static final String CONCEPT_TYPE_THING = "http://www.w3.org/2002/07/owl#Thing";
    public static final String EDGE_LABEL_HAS_SOURCE = "http://visallo.org#hasSource";
    public static final String GEO_LOCATION_RANGE = "http://visallo.org#geolocation";

    public static final StringMetadataVisalloProperty LANGUAGE_METADATA = new StringMetadataVisalloProperty("http://visallo.org#language");
    public static final StringMetadataVisalloProperty TEXT_DESCRIPTION_METADATA = new StringMetadataVisalloProperty("http://visallo.org#textDescription");
    public static final StringMetadataVisalloProperty MIME_TYPE_METADATA = new StringMetadataVisalloProperty("http://visallo.org#mimeType");
    public static final StringMetadataVisalloProperty SOURCE_FILE_NAME_METADATA = new StringMetadataVisalloProperty("http://visallo.org#sourceFileName");
    public static final LongMetadataVisalloProperty SOURCE_FILE_OFFSET_METADATA = new LongMetadataVisalloProperty("http://visallo.org#sourceFileOffset");

    public static final DateSingleValueVisalloProperty MODIFIED_DATE = new DateSingleValueVisalloProperty("http://visallo.org#modifiedDate");
    public static final DateMetadataVisalloProperty MODIFIED_DATE_METADATA = new DateMetadataVisalloProperty("http://visallo.org#modifiedDate");

    public static final DoubleMetadataVisalloProperty CONFIDENCE_METADATA = new DoubleMetadataVisalloProperty("http://visallo.org#confidence");

    public static final VisibilityJsonVisalloProperty VISIBILITY_JSON = new VisibilityJsonVisalloProperty("http://visallo.org#visibilityJson");
    public static final VisibilityJsonMetadataVisalloProperty VISIBILITY_JSON_METADATA = new VisibilityJsonMetadataVisalloProperty("http://visallo.org#visibilityJson");

    public static final StreamingSingleValueVisalloProperty METADATA_JSON = new StreamingSingleValueVisalloProperty("http://visallo.org#metadataJson");
    public static final StreamingVisalloProperty TEXT = new StreamingVisalloProperty("http://visallo.org#text");
    public static final StreamingSingleValueVisalloProperty RAW = new StreamingSingleValueVisalloProperty("http://visallo.org#raw");
    public static final StreamingVisalloProperty CACHED_IMAGE = new StreamingVisalloProperty("http://visallo.org#cached-image");

    public static final StringVisalloProperty GRAPH_PROPERTY_WORKER_WHITE_LIST = new StringVisalloProperty("http://visallo.org#graphPropertyWorkerWhiteList");
    public static final StringVisalloProperty GRAPH_PROPERTY_WORKER_BLACK_LIST = new StringVisalloProperty("http://visallo.org#graphPropertyWorkerBlackList");
    public static final StringSingleValueVisalloProperty CONCEPT_TYPE = new StringSingleValueVisalloProperty("http://visallo.org#conceptType");
    public static final StringVisalloProperty CONTENT_HASH = new StringVisalloProperty("http://visallo.org#contentHash");
    public static final StringVisalloProperty FILE_NAME = new StringVisalloProperty("http://visallo.org#fileName");
    public static final StringVisalloProperty ENTITY_IMAGE_URL = new StringVisalloProperty("http://visallo.org#entityImageUrl");
    public static final StringSingleValueVisalloProperty ENTITY_IMAGE_VERTEX_ID = new StringSingleValueVisalloProperty("http://visallo.org#entityImageVertexId");
    public static final StringVisalloProperty MIME_TYPE = new StringVisalloProperty("http://visallo.org#mimeType");
    public static final StringSingleValueVisalloProperty MODIFIED_BY = new StringSingleValueVisalloProperty("http://visallo.org#modifiedBy");
    public static final StringMetadataVisalloProperty MODIFIED_BY_METADATA = new StringMetadataVisalloProperty("http://visallo.org#modifiedBy");
    public static final PropertyJustificationMetadataSingleValueVisalloProperty JUSTIFICATION = new PropertyJustificationMetadataSingleValueVisalloProperty("http://visallo.org#justification");
    public static final PropertyJustificationMetadataMetadataVisalloProperty JUSTIFICATION_METADATA = new PropertyJustificationMetadataMetadataVisalloProperty("http://visallo.org#justification");
    public static final StringVisalloProperty PROCESS = new StringVisalloProperty("http://visallo.org#process");
    public static final StringVisalloProperty ROW_KEY = new StringVisalloProperty("http://visallo.org#rowKey");
    public static final StringVisalloProperty SOURCE = new StringVisalloProperty("http://visallo.org#source");
    public static final StringVisalloProperty SOURCE_URL = new StringVisalloProperty("http://visallo.org#sourceUrl");
    public static final StringVisalloProperty TITLE = new StringVisalloProperty("http://visallo.org#title");
    public static final StringVisalloProperty COMMENT = new StringVisalloProperty("http://visallo.org/comment#entry");

    public static final DetectedObjectProperty DETECTED_OBJECT = new DetectedObjectProperty("http://visallo.org#detectedObject");

    public static final LongSingleValueVisalloProperty TERM_MENTION_START_OFFSET = new LongSingleValueVisalloProperty("http://visallo.org/termMention#startOffset");
    public static final LongSingleValueVisalloProperty TERM_MENTION_END_OFFSET = new LongSingleValueVisalloProperty("http://visallo.org/termMention#endOffset");
    public static final StringSingleValueVisalloProperty TERM_MENTION_PROCESS = new StringSingleValueVisalloProperty("http://visallo.org/termMention#process");
    public static final StringSingleValueVisalloProperty TERM_MENTION_PROPERTY_KEY = new StringSingleValueVisalloProperty("http://visallo.org/termMention#propertyKey");
    public static final StringSingleValueVisalloProperty TERM_MENTION_RESOLVED_EDGE_ID = new StringSingleValueVisalloProperty("http://visallo.org/termMention#resolvedEdgeId");
    public static final StringSingleValueVisalloProperty TERM_MENTION_TITLE = new StringSingleValueVisalloProperty("http://visallo.org/termMention#title");
    public static final StringSingleValueVisalloProperty TERM_MENTION_CONCEPT_TYPE = new StringSingleValueVisalloProperty("http://visallo.org/termMention#conceptType");
    public static final VisibilityJsonVisalloProperty TERM_MENTION_VISIBILITY_JSON = new VisibilityJsonVisalloProperty("http://visallo.org/termMention#visibilityJson");
    public static final StringSingleValueVisalloProperty TERM_MENTION_REF_PROPERTY_KEY = new StringSingleValueVisalloProperty("http://visallo.org/termMention#ref/propertyKey");
    public static final StringSingleValueVisalloProperty TERM_MENTION_REF_PROPERTY_NAME = new StringSingleValueVisalloProperty("http://visallo.org/termMention#ref/propertyName");
    public static final StringSingleValueVisalloProperty TERM_MENTION_REF_PROPERTY_VISIBILITY = new StringSingleValueVisalloProperty("http://visallo.org/termMention#ref/propertyVisibility");
    public static final StringSingleValueVisalloProperty TERM_MENTION_FOR_ELEMENT_ID = new StringSingleValueVisalloProperty("http://visallo.org/termMention#forElementId");
    public static final TermMentionForProperty TERM_MENTION_FOR_TYPE = new TermMentionForProperty("http://visallo.org/termMention#forType");
    public static final StringSingleValueVisalloProperty TERM_MENTION_SNIPPET = new StringSingleValueVisalloProperty("http://visallo.org/termMention#snippet");
    public static final String TERM_MENTION_LABEL_HAS_TERM_MENTION = "http://visallo.org/termMention#hasTermMention";
    public static final String TERM_MENTION_LABEL_RESOLVED_TO = "http://visallo.org/termMention#resolvedTo";

    private VisalloProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
