package org.visallo.web.structuredingest.spreadsheet;

import org.junit.Test;
import org.visallo.web.structuredingest.core.model.ClientApiAnalysis;
import org.visallo.web.structuredingest.core.util.StructuredFileParserHandler;
import org.visallo.web.structuredingest.core.model.ParseOptions;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class ExcelParserTest {

    @Test
    public void testParseExcel2007Format() throws Exception {
        StructuredFileParserHandler parserHandler = new StructuredFileParserHandler();
        ParseOptions parseOptions = new ParseOptions();

        InputStream input = this.getClass().getResourceAsStream("sample.xls");

        new ExcelParser().ingest(input, parseOptions, parserHandler);
        ClientApiAnalysis info = parserHandler.getResult();

        assertEquals(1, info.sheets.size());
        ClientApiAnalysis.Sheet sheet = info.sheets.get(0);
        assertEquals(3, sheet.columns.size());
        assertEquals("Header1", sheet.columns.get(0).name);
        assertEquals("Header2", sheet.columns.get(1).name);
        assertEquals("Header3", sheet.columns.get(2).name);

        assertEquals(10, sheet.parsedRows.size());
        assertEquals(2, sheet.parsedRows.get(0).columns.size());
        assertEquals("CellA1", sheet.parsedRows.get(0).columns.get(0));
        assertEquals("CellB1", sheet.parsedRows.get(0).columns.get(1));
        assertEquals(2, sheet.parsedRows.get(1).columns.size());
        assertEquals("Cell\"A2\"Value", sheet.parsedRows.get(1).columns.get(0));
        assertEquals("Cell,B2", sheet.parsedRows.get(1).columns.get(1));
    }

    @Test
    public void testParseExcelFormat() throws Exception {
        StructuredFileParserHandler parserHandler = new StructuredFileParserHandler();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.hasHeaderRow = false;

        InputStream input = this.getClass().getResourceAsStream("sample.xlsx");

        new ExcelParser().ingest(input, parseOptions, parserHandler);
        ClientApiAnalysis info = parserHandler.getResult();

        assertEquals(2, info.sheets.size());
        ClientApiAnalysis.Sheet sheet = info.sheets.get(0);
        assertEquals("SampleSheet1", sheet.name);
        assertEquals(0, sheet.columns.size());

        assertEquals(10, sheet.parsedRows.size());
        assertEquals(2, sheet.parsedRows.get(0).columns.size());
        assertEquals("CellA1", sheet.parsedRows.get(0).columns.get(0));
        assertEquals("CellB1", sheet.parsedRows.get(0).columns.get(1));
        assertEquals(2, sheet.parsedRows.get(1).columns.size());
        assertEquals("Cell\"A2\"Value", sheet.parsedRows.get(1).columns.get(0));
        assertEquals("Cell,B2", sheet.parsedRows.get(1).columns.get(1));

        sheet = info.sheets.get(1);
        assertEquals("SampleSheet2", sheet.name);
        assertEquals(0, sheet.columns.size());

        assertEquals(10, sheet.parsedRows.size());
        assertEquals(2, sheet.parsedRows.get(0).columns.size());
        assertEquals("2CellA1", sheet.parsedRows.get(0).columns.get(0));
        assertEquals("2CellB1", sheet.parsedRows.get(0).columns.get(1));
        assertEquals(4, sheet.parsedRows.get(7).columns.size());
        assertEquals("2CellA8", sheet.parsedRows.get(7).columns.get(0));
        assertEquals("2CellB8", sheet.parsedRows.get(7).columns.get(1));
        assertEquals("2CellC8", sheet.parsedRows.get(7).columns.get(2));
        assertEquals("2CellD8", sheet.parsedRows.get(7).columns.get(3));
    }

}