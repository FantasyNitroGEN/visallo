package org.visallo.core.model.properties.types;

public class StringVisalloExtendedData extends VisalloExtendedData<String, String> {
    public StringVisalloExtendedData(String tableName, String columnName) {
        super(tableName, columnName);
    }

    @Override
    public String rawToGraph(String value) {
        return value;
    }

    @Override
    public String graphToRaw(Object value) {
        return value.toString();
    }
}
