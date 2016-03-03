package org.visallo.web;

import com.google.common.base.Joiner;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.MetadataVisalloProperty;
import org.visallo.core.model.properties.types.VisalloPropertyBase;

import java.util.HashMap;
import java.util.Map;

public class WebConfiguration {
    public static final String PREFIX = Configuration.WEB_CONFIGURATION_PREFIX;
    public static final String THROTTLE_MESSAGING_SECONDS = PREFIX + "throttle.messaging.seconds";
    public static final String CACHE_VERTEX_LRU_EXPIRATION_SECONDS = PREFIX + "cache.vertex.lru.expiration.seconds";
    public static final String CACHE_VERTEX_MAX_SIZE = PREFIX + "cache.vertex.max_size";
    public static final String CACHE_EDGE_LRU_EXPIRATION_SECONDS = PREFIX + "cache.edge.lru.expiration.seconds";
    public static final String CACHE_EDGE_MAX_SIZE = PREFIX + "cache.edge.max_size";
    public static final String VERTEX_LOAD_RELATED_MAX_BEFORE_PROMPT = PREFIX + "vertex.loadRelatedMaxBeforePrompt";
    public static final String VERTEX_LOAD_RELATED_MAX_FORCE_SEARCH = PREFIX + "vertex.loadRelatedMaxForceSearch";
    public static final String VERTEX_RELATIONSHIPS_MAX_PER_SECTION = PREFIX + "vertex.relationships.maxPerSection";
    public static final String DETAIL_HISTORY_STACK_MAX = PREFIX + "detail.history.stack.max";
    public static final String VIDEO_PREVIEW_FRAMES_COUNT = PREFIX + "video.preview.frames.count";
    public static final String FIELD_JUSTIFICATION_VALIDATION = PREFIX + "field.justification.validation";
    public static final String SEARCH_DISABLE_WILDCARD_SEARCH = PREFIX + "search.disableWildcardSearch";
    public static final String NOTIFICATIONS_LOCAL_AUTO_DISMISS_SECONDS = PREFIX + "notifications.local.autoDismissSeconds";
    public static final String NOTIFICATIONS_SYSTEM_AUTO_DISMISS_SECONDS = PREFIX + "notifications.system.autoDismissSeconds";
    public static final String NOTIFICATIONS_USER_AUTO_DISMISS_SECONDS = PREFIX + "notifications.user.autoDismissSeconds";
    public static final String TYPEAHEAD_PROPERTIES_MAX_ITEMS = PREFIX + "typeahead.properties.maxItems";
    public static final String TYPEAHEAD_CONCEPTS_MAX_ITEMS = PREFIX + "typeahead.concepts.maxItems";
    public static final String TYPEAHEAD_EDGE_LABELS_MAX_ITEMS = PREFIX + "typeahead.edgeLabels.maxItems";
    public static final String PROPERTIES_MULTIVALUE_DEFAULT_VISIBLE_COUNT = PREFIX + "properties.multivalue.defaultVisibleCount";
    public static final String PROPERTIES_METADATA_PROPERTY_NAMES = PREFIX + "properties.metadata.propertyNames";
    public static final String PROPERTIES_METADATA_PROPERTY_NAMES_DISPLAY = PREFIX + "properties.metadata.propertyNamesDisplay";
    public static final String PROPERTIES_METADATA_PROPERTY_NAMES_TYPE = PREFIX + "properties.metadata.propertyNamesType";
    public static final String MAP_PROVIDER = PREFIX + "map.provider";
    public static final String MAP_PROVIDER_OSM_URL = PREFIX + "map.provider.osm.url";
    public static final String LOGIN_SHOW_POWERED_BY = PREFIX + "login.showPoweredBy";
    public static final String SHOW_VERSION_COMMENTS = PREFIX + "showVersionComments";
    public static final PropertyMetadata PROPERTY_METADATA_SOURCE_TIMEZONE = new PropertyMetadata("http://visallo.org#sourceTimezone",
            "properties.metadata.label.source_timezone",
            "timezone");
    public static final PropertyMetadata PROPERTY_METADATA_MODIFIED_DATE = new PropertyMetadata(VisalloProperties.MODIFIED_DATE,
            "properties.metadata.label.modified_date",
            "datetime");
    public static final PropertyMetadata PROPERTY_METADATA_MODIFIED_BY = new PropertyMetadata(VisalloProperties.MODIFIED_BY,
            "properties.metadata.label.modified_by",
            "user");
    public static final PropertyMetadata PROPERTY_METADATA_STATUS = new PropertyMetadata("sandboxStatus",
            "properties.metadata.label.status",
            "sandboxStatus");
    public static final PropertyMetadata PROPERTY_METADATA_CONFIDENCE = new PropertyMetadata(VisalloProperties.CONFIDENCE_METADATA,
            "properties.metadata.label.confidence",
            "percent");

    public static final Map<String, String> DEFAULTS = new HashMap<>();

    static {
        DEFAULTS.put(LOGIN_SHOW_POWERED_BY, "false");
        DEFAULTS.put(SHOW_VERSION_COMMENTS, "true");

        DEFAULTS.put(THROTTLE_MESSAGING_SECONDS, "2");

        // Local cache rules for vertices / edges (per workspace)
        DEFAULTS.put(CACHE_VERTEX_LRU_EXPIRATION_SECONDS, Integer.toString(10 * 60));
        DEFAULTS.put(CACHE_VERTEX_MAX_SIZE, "500");
        DEFAULTS.put(CACHE_EDGE_LRU_EXPIRATION_SECONDS, Integer.toString(10 * 60));
        DEFAULTS.put(CACHE_EDGE_MAX_SIZE, "250");

        // Load related vertices thresholds
        DEFAULTS.put(VERTEX_LOAD_RELATED_MAX_BEFORE_PROMPT, "50");
        DEFAULTS.put(VERTEX_LOAD_RELATED_MAX_FORCE_SEARCH, "250");

        DEFAULTS.put(VERTEX_RELATIONSHIPS_MAX_PER_SECTION, "5");

        DEFAULTS.put(DETAIL_HISTORY_STACK_MAX, "5");

        DEFAULTS.put(VIDEO_PREVIEW_FRAMES_COUNT, Integer.toString(ArtifactThumbnailRepository.FRAMES_PER_PREVIEW));

        // Justification field validation
        DEFAULTS.put(FIELD_JUSTIFICATION_VALIDATION, JustificationFieldValidation.OPTIONAL.toString());

        // Search
        DEFAULTS.put(SEARCH_DISABLE_WILDCARD_SEARCH, "false");

        // Notifications
        DEFAULTS.put(NOTIFICATIONS_LOCAL_AUTO_DISMISS_SECONDS, "2");
        DEFAULTS.put(NOTIFICATIONS_SYSTEM_AUTO_DISMISS_SECONDS, "-1");
        DEFAULTS.put(NOTIFICATIONS_USER_AUTO_DISMISS_SECONDS, "5");

        DEFAULTS.put(TYPEAHEAD_CONCEPTS_MAX_ITEMS, "-1");
        DEFAULTS.put(TYPEAHEAD_PROPERTIES_MAX_ITEMS, "-1");
        DEFAULTS.put(TYPEAHEAD_EDGE_LABELS_MAX_ITEMS, "-1");

        // Hide multivalue properties after this count
        DEFAULTS.put(PROPERTIES_MULTIVALUE_DEFAULT_VISIBLE_COUNT, "2");

        // Property Metadata shown in info popover
        DEFAULTS.put(PROPERTIES_METADATA_PROPERTY_NAMES, Joiner.on(',').join(PROPERTY_METADATA_SOURCE_TIMEZONE.getName(),
                PROPERTY_METADATA_MODIFIED_DATE.getName(),
                PROPERTY_METADATA_MODIFIED_BY.getName(),
                PROPERTY_METADATA_STATUS.getName(),
                PROPERTY_METADATA_CONFIDENCE.getName()));
        DEFAULTS.put(PROPERTIES_METADATA_PROPERTY_NAMES_DISPLAY, Joiner.on(',').join(PROPERTY_METADATA_SOURCE_TIMEZONE.getMessageKey(),
                PROPERTY_METADATA_MODIFIED_DATE.getMessageKey(),
                PROPERTY_METADATA_MODIFIED_BY.getMessageKey(),
                PROPERTY_METADATA_STATUS.getMessageKey(),
                PROPERTY_METADATA_CONFIDENCE.getMessageKey()));
        DEFAULTS.put(PROPERTIES_METADATA_PROPERTY_NAMES_TYPE, Joiner.on(',').join(PROPERTY_METADATA_SOURCE_TIMEZONE.getDataType(),
                PROPERTY_METADATA_MODIFIED_DATE.getDataType(),
                PROPERTY_METADATA_MODIFIED_BY.getDataType(),
                PROPERTY_METADATA_STATUS.getDataType(),
                PROPERTY_METADATA_CONFIDENCE.getDataType()));

        DEFAULTS.put(MAP_PROVIDER, MapProvider.GOOGLE.toString());
        DEFAULTS.put(MAP_PROVIDER_OSM_URL, "https://a.tile.openstreetmap.org/${z}/${x}/${y}.png," +
                "https://b.tile.openstreetmap.org/${z}/${x}/${y}.png," +
                "https://c.tile.openstreetmap.org/${z}/${x}/${y}.png");
    }

    public static class PropertyMetadata {
        private String name;
        private String messageKey;
        private String dataType;

        public PropertyMetadata(VisalloPropertyBase visalloProperty, String messageKey, String dataType) {
            this(visalloProperty.getPropertyName(), messageKey, dataType);
        }

        public PropertyMetadata(MetadataVisalloProperty visalloProperty, String messageKey, String dataType) {
            this(visalloProperty.getMetadataKey(), messageKey, dataType);
        }

        public PropertyMetadata(String name, String messageKey, String dataType) {
            this.name = name;
            this.messageKey = messageKey;
            this.dataType = dataType;
        }

        public String getName() {
            return name;
        }

        public String getMessageKey() {
            return messageKey;
        }

        public String getDataType() {
            return dataType;
        }
    }

    public enum MapProvider {
        GOOGLE("google"),
        OSM("osm"),
        ARCGIS93REST("ArcGIS93Rest");

        private String string;

        private MapProvider(String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    public enum JustificationFieldValidation {
        REQUIRED,
        OPTIONAL,
        NONE;
    }

    public static JustificationFieldValidation getJustificationFieldValidation(Configuration configuration) {
        return JustificationFieldValidation.valueOf(configuration.get(FIELD_JUSTIFICATION_VALIDATION, DEFAULTS.get(FIELD_JUSTIFICATION_VALIDATION)));
    }

    public static boolean justificationRequired(Configuration configuration) {
        return getJustificationFieldValidation(configuration).equals(JustificationFieldValidation.REQUIRED);
    }
}
