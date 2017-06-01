package org.visallo.core.model.properties.types;

import org.vertexium.ExtendedDataRow;

public class DoubleVisalloExtendedData extends IdentityVisalloExtendedData<Double> {
    public DoubleVisalloExtendedData(String tableName, String propertyName) {
        super(tableName, propertyName);
    }

    public double getValue(ExtendedDataRow row, double defaultValue) {
        Double nullable = getValue(row);
        if (nullable == null) {
            return defaultValue;
        }
        return nullable;
    }
}
