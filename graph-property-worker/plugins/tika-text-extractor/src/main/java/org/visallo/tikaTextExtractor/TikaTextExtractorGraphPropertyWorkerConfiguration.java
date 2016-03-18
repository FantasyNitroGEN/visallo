package org.visallo.tikaTextExtractor;

import com.google.inject.Inject;
import org.vertexium.Element;
import org.vertexium.Property;
import org.visallo.core.config.Configurable;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.properties.VisalloProperties;

import java.util.Map;

public class TikaTextExtractorGraphPropertyWorkerConfiguration {
    public static final String CONFIGURATION_PREFIX = TikaTextExtractorGraphPropertyWorker.class.getName();
    public static final String TEXT_EXTRACT_MAPPING_CONFIGURATION_PREFIX = CONFIGURATION_PREFIX + ".textExtractMapping";
    public static final String DEFAULT_TEXT_EXTRACT_MAPPING = "raw";

    private final Map<String, TextExtractMapping> textExtractMappings;

    @Inject
    public TikaTextExtractorGraphPropertyWorkerConfiguration(Configuration configuration) {
        textExtractMappings = configuration.getMultiValueConfigurables(TEXT_EXTRACT_MAPPING_CONFIGURATION_PREFIX, TextExtractMapping.class);

        if (!textExtractMappings.containsKey(DEFAULT_TEXT_EXTRACT_MAPPING)) {
            TextExtractMapping textExtractMapping = new TextExtractMapping();
            textExtractMapping.rawPropertyName = VisalloProperties.RAW.getPropertyName();
            textExtractMapping.extractedTextPropertyName = VisalloProperties.TEXT.getPropertyName();
            textExtractMapping.textDescription = "Extracted Text";
            textExtractMappings.put(DEFAULT_TEXT_EXTRACT_MAPPING, textExtractMapping);
        }
    }

    boolean isHandled(Element element, Property property) {
        for (TextExtractMapping textExtractMapping : this.textExtractMappings.values()) {
            if (textExtractMapping.rawPropertyName.equals(property.getName())) {
                return true;
            }
        }
        return false;
    }

    TextExtractMapping getTextExtractMapping(Element element, Property property) {
        for (TextExtractMapping textExtractMapping : this.textExtractMappings.values()) {
            if (textExtractMapping.rawPropertyName.equals(property.getName())) {
                return textExtractMapping;
            }
        }
        return null;
    }

    public static class TextExtractMapping {
        @Configurable()
        private String rawPropertyName;

        @Configurable()
        private String extractedTextPropertyName;

        @Configurable(required = false)
        private String textDescription;

        public String getRawPropertyName() {
            return rawPropertyName;
        }

        public String getExtractedTextPropertyName() {
            return extractedTextPropertyName;
        }

        public String getTextDescription() {
            return textDescription;
        }
    }
}
