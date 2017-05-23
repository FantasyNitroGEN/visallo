package org.visallo.core.model.properties.types;

import org.vertexium.ExtendedDataRow;

public class IntegerVisalloExtendedData extends IdentityVisalloExtendedData<Integer> {
    public IntegerVisalloExtendedData(String tableName, String propertyName) {
        super(tableName, propertyName);
    }

    public int getValue(ExtendedDataRow row, int defaultValue) {
        Integer nullable = getValue(row);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
