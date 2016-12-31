package org.visallo.web.structuredingest.spreadsheet;

import com.google.common.collect.Sets;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.structuredingest.core.model.ClientApiAnalysis;
import org.visallo.web.structuredingest.core.util.StructuredFileParserHandler;
import org.visallo.web.structuredingest.core.model.StructuredIngestParser;
import org.visallo.web.structuredingest.core.util.BaseStructuredFileParserHandler;
import org.visallo.web.structuredingest.core.model.ParseOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ExcelParser extends BaseParser implements StructuredIngestParser {

    @Override
    public Set<String> getSupportedMimeTypes() {
        return Sets.newHashSet(
                "application/xls",
                "application/excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
    }

    @Override
    public void ingest(InputStream in, ParseOptions parseOptions, BaseStructuredFileParserHandler parserHandler) throws Exception {
        parseExcel(in, parseOptions, parserHandler);
    }

    @Override
    public ClientApiAnalysis analyze(InputStream inputStream) throws Exception {
        StructuredFileParserHandler handler = new StructuredFileParserHandler();
        handler.getHints().sendColumnIndices = true;
        handler.getHints().allowHeaderSelection = true;

        ParseOptions options = new ParseOptions();
        options.hasHeaderRow = false;
        parseExcel(inputStream, options, handler);
        return handler.getResult();
    }

    private void parseExcel(InputStream in, ParseOptions options, BaseStructuredFileParserHandler handler) {
        try {
            Workbook workbook = WorkbookFactory.create(in);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(true);

            int numSheets = workbook.getNumberOfSheets();
            for(int i = 0; i < numSheets; i++) {
                if (options.sheetIndex != null && i != options.sheetIndex) continue;
                Sheet excelSheet = workbook.getSheetAt(i);
                handler.newSheet(excelSheet.getSheetName());

                if(excelSheet.getPhysicalNumberOfRows() > 0) {
                    int lastRowNum = excelSheet.getLastRowNum();
                    handler.setTotalRows(lastRowNum);
                    for(int j = 0, rowIndex = 0; j <= lastRowNum; j++) {
                        if (rowIndex < options.startRowIndex) {
                            rowIndex++;
                            continue;
                        }

                        Row row = excelSheet.getRow(j);
                        List<Object> parsedRow = parseExcelRow(row, evaluator, formatter);
                        if(parsedRow.size() > 0) {
                            if(rowIndex == options.startRowIndex && options.hasHeaderRow) {
                                for (int k = 0; k < parsedRow.size(); k++) {
                                    handler.addColumn(parsedRow.get(k).toString());
                                }
                            } else {
                                if(!handler.addRow(parsedRow, j)) {
                                    break;
                                }
                            }

                            rowIndex++;
                        }
                    }
                }
            }
        } catch (IOException ex) {
            throw new VisalloException("Could not read excel workbook", ex);
        } catch (InvalidFormatException e) {
            throw new VisalloException("Could not read excel workbook", e);
        }
    }

    private List<Object> parseExcelRow(Row row, FormulaEvaluator evaluator, DataFormatter formatter) {
        List<Object> parsedRow = new ArrayList<Object>();

        if(row != null) {
            int lastCellNum = row.getLastCellNum();
            for(int i = 0; i < lastCellNum; i++) {
                Cell cell = row.getCell(i);
                String cellValue = "";
                if(cell != null) {
                    if(cell.getCellType() != Cell.CELL_TYPE_FORMULA) {
                        cellValue = formatter.formatCellValue(cell);
                    }
                    else {
                        cellValue = formatter.formatCellValue(cell, evaluator);
                    }
                }

                parsedRow.add(cellValue);
            }
        }

        return parsedRow;
    }

}
