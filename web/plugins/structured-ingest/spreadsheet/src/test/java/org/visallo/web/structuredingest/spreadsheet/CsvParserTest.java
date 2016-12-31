package org.visallo.web.structuredingest.spreadsheet;

import org.junit.Test;
import org.visallo.web.structuredingest.core.model.ClientApiAnalysis;
import org.visallo.web.structuredingest.core.util.StructuredFileParserHandler;
import org.visallo.web.structuredingest.core.model.ParseOptions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class CsvParserTest {

    private InputStream toStream(String data) throws Exception {
        return new ByteArrayInputStream(data.getBytes("UTF-8"));
    }

    @Test
    public void testParse() throws Exception {
        String data = "last,first\n"
                + "Appleseed,Johnny\n"
                + "B.,Jill";
        StructuredFileParserHandler parserHandler = new StructuredFileParserHandler();
        ParseOptions parseOptions = new ParseOptions();
        new CsvParser().ingest(toStream(data), parseOptions, parserHandler);
        ClientApiAnalysis info = parserHandler.getResult();

        assertEquals(1, info.sheets.size());
        ClientApiAnalysis.Sheet sheet = info.sheets.get(0);
        assertEquals(2, sheet.columns.size());
        assertEquals("last", sheet.columns.get(0).name);
        assertEquals("first", sheet.columns.get(1).name);

        assertEquals(2, sheet.parsedRows.size());
        assertEquals(2, sheet.parsedRows.get(0).columns.size());
        assertEquals("Appleseed", sheet.parsedRows.get(0).columns.get(0));
        assertEquals("Johnny", sheet.parsedRows.get(0).columns.get(1));
        assertEquals(2, sheet.parsedRows.get(1).columns.size());
        assertEquals("B.", sheet.parsedRows.get(1).columns.get(0));
        assertEquals("Jill", sheet.parsedRows.get(1).columns.get(1));
    }

    @Test
    public void testParseCSVSkipsBlankRows() throws Exception {
        String data = "last,first\n"
                + "                \n"
                + "       ,         \n"
                + "Appleseed,Johnny";
        StructuredFileParserHandler parserHandler = new StructuredFileParserHandler();
        ParseOptions parseOptions = new ParseOptions();
        new CsvParser().ingest(toStream(data), parseOptions, parserHandler);
        ClientApiAnalysis info = parserHandler.getResult();

        assertEquals(1, info.sheets.size());
        ClientApiAnalysis.Sheet sheet = info.sheets.get(0);
        assertEquals(2, sheet.columns.size());
        assertEquals("last", sheet.columns.get(0).name);
        assertEquals("first", sheet.columns.get(1).name);

        assertEquals(1, sheet.parsedRows.size());
        assertEquals(2, sheet.parsedRows.get(0).columns.size());
        assertEquals("Appleseed", sheet.parsedRows.get(0).columns.get(0));
        assertEquals("Johnny", sheet.parsedRows.get(0).columns.get(1));
    }
}