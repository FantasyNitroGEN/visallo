package org.visallo.web.structuredingest.core.routes;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.visallo.web.structuredingest.core.model.ClientApiMimeTypes;
import org.visallo.web.structuredingest.core.model.StructuredIngestParser;
import org.visallo.web.structuredingest.core.util.StructuredIngestParserFactory;

import java.util.Collection;

public class MimeTypes implements ParameterizedHandler {

    private final StructuredIngestParserFactory structuredIngestParserFactory;

    @Inject
    public MimeTypes(
            StructuredIngestParserFactory structuredIngestParserFactory
    ) {
        this.structuredIngestParserFactory = structuredIngestParserFactory;
    }

    @Handle
    public ClientApiMimeTypes handle() throws Exception {
        Collection<StructuredIngestParser> parsers = structuredIngestParserFactory.getParsers();
        ClientApiMimeTypes response = new ClientApiMimeTypes();
        for (StructuredIngestParser parser : parsers) {
            response.addMimeTypes(parser.getSupportedMimeTypes());
        }
        return response;
    }
}
