package org.visallo.web.structuredingest.core.util;

import org.visallo.web.structuredingest.core.util.mapping.ColumnMappingType;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BaseStructuredFileParserHandler {
    private long totalRows = -1;
    public void newSheet(String name) {
    }

    public void addColumn(String title) {
        addColumn(title, ColumnMappingType.Unknown);
    }

    public void addColumn(String name, ColumnMappingType type) {

    }

    public boolean addRow(List<Object> values, long rowNum) {
        Map<String, Object> sortedMap = new TreeMap<String, Object>();

        Long i = 0l;
        for (Object value : values) {
            sortedMap.put(i.toString(), value);
            i++;
        }

        return addRow(sortedMap, rowNum);
    }

    public boolean addRow(Map<String, Object> row, long rowNum) {
        return true;
    }

    public void setTotalRows(long rows) {
        this.totalRows = rows;
    }

    public long getTotalRows() {
        return totalRows;
    }
}
