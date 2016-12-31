package org.visallo.web.structuredingest.core.util;

import com.google.inject.Inject;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.structuredingest.core.model.StructuredIngestParser;

import java.util.Collection;
import java.util.Set;

public class StructuredIngestParserFactory {

    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(StructuredIngestParserFactory.class);

    private final Configuration configuration;

    @Inject
    public StructuredIngestParserFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    public Collection<StructuredIngestParser> getParsers() {
        return InjectHelper.getInjectedServices(StructuredIngestParser.class, configuration);
    }

    public StructuredIngestParser getParser(String mimeType) {
        Collection<StructuredIngestParser> parsers = getParsers();
        for (StructuredIngestParser parser : parsers) {
            Set<String> supported = parser.getSupportedMimeTypes();
            if (supported.size() == 0) {
                LOGGER.warn("Parsers should support at least one mimeType: %s", parser.getClass().getName());
            } else if (supported.stream().anyMatch(s -> s.toLowerCase().equals(mimeType.toLowerCase()))) {
                return parser;
            }
        }
        return null;
    }
}
