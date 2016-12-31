package org.visallo.web.structuredingest.core.util;

import org.visallo.core.exception.VisalloException;
import org.visallo.web.structuredingest.core.model.ClientApiAnalysis;
import org.visallo.web.structuredingest.core.util.mapping.ColumnMappingType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StructuredFileParserHandler extends BaseStructuredFileParserHandler {
    private ClientApiAnalysis result = new ClientApiAnalysis();
    private ClientApiAnalysis.Sheet currentSheet;

    public ClientApiAnalysis getResult() {
        return this.result;
    }

    @Override
    public void newSheet(String name) {
        currentSheet = new ClientApiAnalysis.Sheet();
        currentSheet.name = name;
        result.sheets.add(currentSheet);
    }

    public ClientApiAnalysis.Hints getHints() {
        return result.hints;
    }

    @Override
    public void addColumn(String name, ColumnMappingType type) {
        ClientApiAnalysis.Column column = new ClientApiAnalysis.Column();
        column.name = name;
        column.type = type;
        currentSheet.columns.add(column);
    }

    @Override
    public boolean addRow(List<Object> values, long rowNum) {
        ClientApiAnalysis.ParsedRow parsedRow = new ClientApiAnalysis.ParsedRow();
        parsedRow.columns.addAll(values);
        currentSheet.parsedRows.add(parsedRow);
        return currentSheet.parsedRows.size() < 10;
    }

    @Override
    public void setTotalRows(long rows) {
        super.setTotalRows(rows);
        currentSheet.totalRows = rows;
    }

    @Override
    public boolean addRow(Map<String, Object> row, long rowNum) {
        if (currentSheet.columns.size() == 0) {
            throw new VisalloException("Set columns before rows");
        }
        List<Object> values = new ArrayList<>(row.size());
        for (ClientApiAnalysis.Column column : currentSheet.columns) {
            Object value = row.get(column.name);
            values.add(value == null ? "" : value);
        }

        return addRow(values, rowNum);
    }
}
