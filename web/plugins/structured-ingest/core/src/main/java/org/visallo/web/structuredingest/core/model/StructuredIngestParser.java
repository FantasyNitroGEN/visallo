package org.visallo.web.structuredingest.core.model;

import org.visallo.web.structuredingest.core.util.BaseStructuredFileParserHandler;

import java.io.InputStream;
import java.util.Set;

public interface StructuredIngestParser {

    Set<String> getSupportedMimeTypes();

    void ingest(InputStream inputStream, ParseOptions parseOptions, BaseStructuredFileParserHandler parserHandler) throws Exception;

    ClientApiAnalysis analyze(InputStream inputStream) throws Exception;
}
