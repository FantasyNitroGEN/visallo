package org.visallo.vertexium.mapreduce;

import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.accumulo.core.data.Mutation;
import org.apache.hadoop.io.Text;
import org.vertexium.accumulo.mapreduce.ElementMapper;
import org.vertexium.id.IdGenerator;
import org.vertexium.id.UUIDIdGenerator;

import java.io.IOException;

public abstract class VisalloElementMapperBase<KEYIN, VALUEIN> extends ElementMapper<KEYIN, VALUEIN, Text, Mutation> {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(VisalloElementMapperBase.class);
    private IdGenerator idGenerator = new UUIDIdGenerator(null);

    @Override
    protected void map(KEYIN key, VALUEIN line, Context context) {
        try {
            safeMap(key, line, context);
        } catch (Throwable ex) {
            LOGGER.error("failed mapping " + key, ex);
        }
    }

    protected abstract void safeMap(KEYIN key, VALUEIN line, Context context) throws Exception;

    @Override
    protected void saveDataMutation(Context context, Text dataTableName, Mutation m) throws IOException, InterruptedException {
        context.write(getKey(context, dataTableName, m), m);
    }

    @Override
    protected void saveEdgeMutation(Context context, Text edgesTableName, Mutation m) throws IOException, InterruptedException {
        context.write(getKey(context, edgesTableName, m), m);
    }

    @Override
    protected void saveVertexMutation(Context context, Text verticesTableName, Mutation m) throws IOException, InterruptedException {
        context.write(getKey(context, verticesTableName, m), m);
    }

    protected Text getKey(Context context, Text tableName, Mutation m) {
        return tableName;
    }

    @Override
    public IdGenerator getIdGenerator() {
        return idGenerator;
    }
}
