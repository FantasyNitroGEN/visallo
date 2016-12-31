package org.visallo.web.structuredingest.core.model;

import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.structuredingest.core.util.mapping.ColumnMappingType;

import java.util.ArrayList;
import java.util.List;

public class ClientApiAnalysis implements ClientApiObject {
    public List<Sheet> sheets = new ArrayList<>();
    public Hints hints = new Hints();

    public static class Sheet {
        public String name;
        public List<Column> columns = new ArrayList<>();
        public List<ParsedRow> parsedRows = new ArrayList<>();
        public long totalRows;
    }

    public static class Column {
        public String name;
        public ColumnMappingType type;
    }

    public static class ParsedRow {
        public List<Object> columns = new ArrayList<>();
    }

    public static class Hints {
        public boolean allowHeaderSelection = true;
        public boolean sendColumnIndices = false;
    }
}
