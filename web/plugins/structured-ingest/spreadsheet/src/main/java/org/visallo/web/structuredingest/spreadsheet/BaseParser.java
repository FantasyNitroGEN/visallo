package org.visallo.web.structuredingest.spreadsheet;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.structuredingest.core.model.ParseOptions;

import java.io.*;

public abstract class BaseParser {

    protected boolean rowIsBlank(String[] columnValues) {
        // skip over blank rows
        boolean allBlank = true;
        for (int i = 0; i < columnValues.length && allBlank; i++) {
            allBlank = allBlank && StringUtils.isBlank(columnValues[i]);
        }
        return allBlank;
    }

    protected int getTotalRows(InputStream in, ParseOptions options) {
        try (Reader reader = new InputStreamReader(in)) {
            int row = 0;
            try (CSVReader csvReader = new CSVReader(reader, options.separator, options.quoteChar)) {
                String[] columnValues;
                while ((columnValues = csvReader.readNext()) != null) {
                    if (rowIsBlank(columnValues)) {
                        continue;
                    }
                    row++;
                }
                in.reset();
                return row;
            }
        } catch (IOException e) {
            throw new VisalloException("Could not read csv", e);
        }

    }

}
