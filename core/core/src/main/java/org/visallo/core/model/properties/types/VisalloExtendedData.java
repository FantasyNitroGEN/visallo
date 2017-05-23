package org.visallo.core.model.properties.types;

import org.vertexium.Element;
import org.vertexium.ExtendedDataRow;
import org.vertexium.mutation.ElementMutation;
import org.visallo.core.model.graph.ElementUpdateContext;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class VisalloExtendedData<TRaw, TGraph> {
    private static final String ROW_ID_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private final String tableName;
    private final String columnName;

    protected VisalloExtendedData(String tableName, String columnName) {
        this.tableName = tableName;
        this.columnName = columnName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public abstract TGraph rawToGraph(TRaw value);

    public abstract TRaw graphToRaw(Object value);

    public <T extends Element> void addExtendedData(
            ElementUpdateContext<T> elemCtx,
            String row,
            TRaw newValue,
            PropertyMetadata propertyMetadata
    ) {
        addExtendedData(elemCtx.getMutation(), row, newValue, propertyMetadata);
    }

    public <T extends Element> void addExtendedData(
            ElementUpdateContext<T> elemCtx,
            String row,
            TRaw newValue,
            PropertyMetadata propertyMetadata,
            Long timestamp
    ) {
        addExtendedData(elemCtx.getMutation(), row, newValue, propertyMetadata, timestamp);
    }

    public <T extends Element> void addExtendedData(
            ElementMutation<T> m,
            String row,
            TRaw newValue,
            PropertyMetadata propertyMetadata
    ) {
        addExtendedData(m, row, newValue, propertyMetadata, null);
    }

    public <T extends Element> void addExtendedData(
            ElementMutation<T> m,
            String row,
            TRaw newValue,
            PropertyMetadata propertyMetadata,
            Long timestamp
    ) {
        checkNotNull(newValue, "null values are not allowed");
        m.addExtendedData(tableName, row, columnName, rawToGraph(newValue), timestamp, propertyMetadata.getVisibility());
    }

    public static String rowIdFromDate(Date timestamp) {
        return new SimpleDateFormat(ROW_ID_DATE_FORMAT).format(timestamp);
    }

    public TRaw getValue(ExtendedDataRow row) {
        Object value = row.getPropertyValue(columnName);
        if (value == null) {
            return null;
        }
        return graphToRaw(value);
    }
}
