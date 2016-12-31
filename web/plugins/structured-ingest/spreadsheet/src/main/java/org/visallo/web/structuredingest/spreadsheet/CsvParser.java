package org.visallo.web.structuredingest.spreadsheet;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.Sets;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.structuredingest.core.model.ClientApiAnalysis;
import org.visallo.web.structuredingest.core.util.StructuredFileParserHandler;
import org.visallo.web.structuredingest.core.model.StructuredIngestParser;
import org.visallo.web.structuredingest.core.util.BaseStructuredFileParserHandler;
import org.visallo.web.structuredingest.core.model.ParseOptions;

import java.io.*;
import java.util.Arrays;
import java.util.Set;

public class CsvParser extends BaseParser implements StructuredIngestParser {

    private final static String CSV_MIME_TYPE = "text/csv";

    @Override
    public Set<String> getSupportedMimeTypes() {
        return Sets.newHashSet(CSV_MIME_TYPE);
    }

    @Override
    public void ingest(InputStream in, ParseOptions parseOptions, BaseStructuredFileParserHandler parserHandler) throws Exception {
        parseCsvSheet(in, parseOptions, parserHandler);
    }

    @Override
    public ClientApiAnalysis analyze(InputStream inputStream) throws Exception {
        StructuredFileParserHandler handler = new StructuredFileParserHandler();
        handler.getHints().sendColumnIndices = true;
        handler.getHints().allowHeaderSelection = true;

        ParseOptions options = new ParseOptions();
        options.hasHeaderRow = false;
        parseCsvSheet(inputStream, options, handler);
        return handler.getResult();
    }

    private void parseCsvSheet(InputStream in, ParseOptions options, BaseStructuredFileParserHandler handler) {
        handler.newSheet("");

        handler.setTotalRows(getTotalRows(in, options));

        try (Reader reader = new InputStreamReader(in)) {
            int row = 0;
            try (CSVReader csvReader = new CSVReader(reader, options.separator, options.quoteChar)) {
                String[] columnValues;

                while ((columnValues = csvReader.readNext()) != null) {
                    if (row < options.startRowIndex) {
                        row++;
                        continue;
                    }
                    if (rowIsBlank(columnValues)) {
                        continue;
                    }

                    if (row == options.startRowIndex && options.hasHeaderRow) {
                        for (String headerColumn : columnValues) {
                            handler.addColumn(headerColumn);
                        }
                    } else {
                        if (!handler.addRow(Arrays.asList(columnValues), row)) {
                            break;
                        }
                    }
                    row++;
                }
            }
        } catch (IOException ex) {
            throw new VisalloException("Could not read csv", ex);
        }
    }
}

