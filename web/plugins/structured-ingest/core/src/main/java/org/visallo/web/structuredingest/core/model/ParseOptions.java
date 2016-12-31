package org.visallo.web.structuredingest.core.model;

import org.json.JSONObject;

public class ParseOptions {
    public boolean hasHeaderRow;
    public int startRowIndex;
    public Integer sheetIndex;
    public char separator;
    public char quoteChar;

    public ParseOptions() {
        hasHeaderRow = true;
        startRowIndex = 0;
        sheetIndex = null;
        separator = ',';
        quoteChar = '"';
    }

    public ParseOptions(String json) {
        this();
        if (json != null) {
            JSONObject o = new JSONObject(json);
            hasHeaderRow = o.optBoolean("hasHeaderRow", true);
            startRowIndex = o.optInt("startRowIndex", 0);
            sheetIndex = o.optInt("sheetIndex", 0);
            quoteChar = o.optString("quoteChar", "\"").charAt(0);
            separator = o.optString("separator", ",").charAt(0);
        }
    }
}
