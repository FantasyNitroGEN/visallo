package org.visallo.core.ingest.graphProperty;

import com.google.inject.Inject;
import org.vertexium.Element;
import org.vertexium.Property;
import org.visallo.core.config.Configurable;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.properties.VisalloProperties;

import java.util.HashSet;
import java.util.Set;

public class MimeTypeGraphPropertyWorkerConfiguration {
    public static final String CONFIGURATION_PREFIX = MimeTypeGraphPropertyWorker.class.getName();
    public static final String HANDLED_CONFIGURATION_PREFIX = CONFIGURATION_PREFIX + ".handled";

    private Set<String> handledPropertyNames = new HashSet<>();

    @Inject
    public MimeTypeGraphPropertyWorkerConfiguration(Configuration configuration) {
        for (Handled h : configuration.getMultiValueConfigurables(HANDLED_CONFIGURATION_PREFIX, Handled.class).values()) {
            handledPropertyNames.add(h.getPropertyName());
        }

        handledPropertyNames.add(VisalloProperties.RAW.getPropertyName());
    }

    public boolean isHandled(Element element, Property property) {
        return handledPropertyNames.contains(property.getName());
    }

    public static class Handled {
        @Configurable
        private String propertyName;

        public String getPropertyName() {
            return propertyName;
        }
    }
}
