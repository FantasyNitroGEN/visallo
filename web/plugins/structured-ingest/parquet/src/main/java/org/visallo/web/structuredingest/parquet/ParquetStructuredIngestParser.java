package org.visallo.web.structuredingest.parquet;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jodd.datetime.JDateTime;
import jodd.datetime.JulianDateStamp;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.format.converter.ParquetMetadataConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.api.ReadSupport;
import org.apache.parquet.hadoop.metadata.BlockMetaData;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.OriginalType;
import org.apache.parquet.schema.Type;
import org.apache.parquet.tools.read.SimpleReadSupport;
import org.apache.parquet.tools.read.SimpleRecord;
import org.vertexium.util.StreamUtils;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.structuredingest.core.model.ClientApiAnalysis;
import org.visallo.web.structuredingest.core.model.ParseOptions;
import org.visallo.web.structuredingest.core.model.StructuredIngestParser;
import org.visallo.web.structuredingest.core.util.BaseStructuredFileParserHandler;
import org.visallo.web.structuredingest.core.util.StructuredFileParserHandler;
import org.visallo.web.structuredingest.core.util.mapping.ColumnMappingType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class ParquetStructuredIngestParser implements StructuredIngestParser {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ParquetStructuredIngestParser.class);
    private static final String PARQUET_MIME_TYPE = "application/x-parquet";
    private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000L;
    private static final long NANOS_PER_DAY = MILLIS_PER_DAY * 1000000;

    @Override
    public Set<String> getSupportedMimeTypes() {
        return Sets.newHashSet(PARQUET_MIME_TYPE);
    }

    @Override
    public void ingest(InputStream in, ParseOptions parseOptions, BaseStructuredFileParserHandler parserHandler) throws Exception {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("parquet", "tmp");
            StreamUtils.copy(in, new FileOutputStream(tempFile));

            Configuration conf = new Configuration();
            Path path = new Path(tempFile.getAbsolutePath());
            ParquetMetadata metaData = ParquetFileReader.readFooter(conf, path, ParquetMetadataConverter.NO_FILTER);
            MessageType schema = metaData.getFileMetaData().getSchema();
            long rowCount = metaData.getBlocks().stream().mapToLong(BlockMetaData::getRowCount).sum();
            parserHandler.setTotalRows(rowCount);

            try (ParquetReader<SimpleRecord> reader = ParquetReader.builder(new SimpleReadSupport(), new Path(tempFile.getAbsolutePath())).build()) {
                parserHandler.newSheet("");
                int rowNum = 0;

                for (SimpleRecord value = reader.read(); value != null; value = reader.read()) {
                    Map<String, Object> row = Maps.newHashMap();

                    for (SimpleRecord.NameValue nameValue : value.getValues()) {
                        String name = nameValue.getName();
                        Object val = nameValue.getValue();
                        if (!(val instanceof SimpleRecord)) {
                            Type type = schema.getType(schema.getFieldIndex(name));
                            row.put(name, getRecordValue(val, type));
                        }
                    }

                    if (!parserHandler.addRow(row, rowNum++)) break;
                }
            }
        } finally {
            if (tempFile != null) tempFile.delete();
        }
    }

    @Override
    public ClientApiAnalysis analyze(InputStream inputStream) throws Exception {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("parquet", "tmp");
            StreamUtils.copy(inputStream, new FileOutputStream(tempFile));

            Configuration conf = new Configuration();
            Path path = new Path(tempFile.getAbsolutePath());
            ParquetMetadata metaData = ParquetFileReader.readFooter(conf, path, ParquetMetadataConverter.NO_FILTER);
            MessageType schema = metaData.getFileMetaData().getSchema();
            StructuredFileParserHandler parserHandler = new StructuredFileParserHandler();
            parserHandler.newSheet("");
            parserHandler.getHints().allowHeaderSelection = false;
            parserHandler.getHints().sendColumnIndices = false;

            for (ColumnDescriptor col : schema.getColumns()) {
                String[] colPath = col.getPath();
                if (colPath.length == 0) {
                    throw new VisalloException("structured file path has no elements");
                }

                if (colPath.length > 1) {
                    LOGGER.warn("not parsing nested element %s", Joiner.on(",").join(col.getPath()));
                    continue;
                }

                parserHandler.addColumn(colPath[0], getColumnType(schema.getType(colPath)));
            }

            DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            ReadSupport readSupport = new SimpleReadSupport();
            try (ParquetReader<SimpleRecord> reader = ParquetReader.builder(readSupport, path).build()) {
                int rowNum = 0;

                for (SimpleRecord value = reader.read(); value != null; value = reader.read()) {
                    Map<String, Object> row = Maps.newHashMap();

                    for (SimpleRecord.NameValue nameValue : value.getValues()) {
                        String name = nameValue.getName();
                        Object val = nameValue.getValue();
                        if (!(val instanceof SimpleRecord)) {
                            Type type = schema.getType(schema.getFieldIndex(name));
                            Object recordValue = getRecordValue(val, type);
                            String strValue;
                            if (recordValue instanceof Date) {
                                strValue = df.format((Date) recordValue);
                            } else {
                                strValue = recordValue.toString();
                            }
                            row.put(name, strValue);
                        }
                    }

                    if (!parserHandler.addRow(row, rowNum++)) break;
                }


                long rowCount = metaData.getBlocks().stream().mapToLong(BlockMetaData::getRowCount).sum();
                parserHandler.setTotalRows(rowCount);
                return parserHandler.getResult();
            }
        } finally {
            if (tempFile != null) tempFile.delete();
        }
    }



    private Object getRecordValue(Object value, Type type) {
        String primitive = type.asPrimitiveType().getPrimitiveTypeName().toString();

        // Special handling for INT96 "Timestamp" columns
        if (primitive.equals("INT96")) {
            Date date = getDateFromInt96(value);
            if (date != null) {
                return date;
            }
        }

        ColumnMappingType columnType = getColumnType(type);
        switch (columnType) {
            case Boolean:
                return Boolean.parseBoolean(value.toString());

            case Date:
                Calendar c = new GregorianCalendar();
                c.setTimeZone(TimeZone.getTimeZone("UTC"));
                c.setTime(new Date(0));
                c.add(Calendar.DAY_OF_YEAR, (Integer) value);
                return c.getTime();

            case DateTime:
                return new Date((Long) value);

            case GeoPoint:
                break;
            case Number:
                break;

            case String:
            case Unknown:
            default:

        }

        return value.toString();
    }

    private ColumnMappingType getColumnType(Type type) {
        OriginalType originalType = type.getOriginalType();
        if (originalType == null) {

            String primitiveType = type.asPrimitiveType().getPrimitiveTypeName().toString();
            if (primitiveType.equals("BOOLEAN")) return ColumnMappingType.Boolean;
            if (primitiveType.equals("DECIMAL")) return ColumnMappingType.Number;
            if (primitiveType.equals("DOUBLE")) return ColumnMappingType.Number;
            if (primitiveType.equals("INT96")) return ColumnMappingType.Date;
            if (primitiveType.indexOf("INT") == 0) return ColumnMappingType.Number;
            if (primitiveType.indexOf("UINT") == 0) return ColumnMappingType.Number;

            return ColumnMappingType.Unknown;
        }
        String original = originalType.toString();

        if (original.equals("DATE")) return ColumnMappingType.Date;
        if (original.equals("TIMESTAMP_MILLIS")) return ColumnMappingType.DateTime;
        if (original.indexOf("UTF") == 0) return ColumnMappingType.String;


        return ColumnMappingType.Unknown;
    }

    private Date getDateFromInt96(Object val) {
        if (val instanceof byte[]) {
            byte[] bytes = (byte[]) val;
            ByteBuffer buf = Binary.fromByteArray(bytes).toByteBuffer();
            buf.order(ByteOrder.LITTLE_ENDIAN);
            long timeOfDayNanos = buf.getLong();
            int julianDay = buf.getInt();

            JDateTime date = new JDateTime(new JulianDateStamp(julianDay, (double) timeOfDayNanos / NANOS_PER_DAY));
            return date.convertToDate();
        }
        return null;
    }
}
