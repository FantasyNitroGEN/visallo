package org.visallo.reindexmr;


import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.vertexium.Authorizations;
import org.vertexium.Element;
import org.vertexium.GraphFactory;
import org.vertexium.accumulo.AccumuloAuthorizations;
import org.vertexium.accumulo.AccumuloGraph;
import org.vertexium.accumulo.mapreduce.VertexiumMRUtils;
import org.vertexium.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReindexMRMapper extends Mapper<Text, Element, Object, Element> {
    private static VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ReindexMRMapper.class);
    private static final int DEFAULT_BATCH_SIZE = 100;
    private AccumuloGraph graph;
    private Authorizations authorizations;
    private List<Element> elementCache;
    private int batchSize;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        LOGGER = VisalloLoggerFactory.getLogger(ReindexMRMapper.class);
        super.setup(context);
        LOGGER.info("setup: " + toString(context.getInputSplit().getLocations()));
        Map configurationMap = VertexiumMRUtils.toMap(context.getConfiguration());
        batchSize = context.getConfiguration().getInt("reindex.batchsize", DEFAULT_BATCH_SIZE);
        elementCache = new ArrayList<>(batchSize);
        this.graph = (AccumuloGraph) new GraphFactory().createGraph(MapUtils.getAllWithPrefix(configurationMap, "graph"));
        this.authorizations = new AccumuloAuthorizations(context.getConfiguration().getStrings(VertexiumMRUtils.CONFIG_AUTHORIZATIONS));
    }

    private String toString(String[] locations) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < locations.length; i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(locations[i]);
        }
        return result.toString();
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        writeCache();
        LOGGER.info("cleanup");
        graph.shutdown();
        super.cleanup(context);
    }

    @Override
    protected void map(Text rowKey, Element element, Context context) throws IOException, InterruptedException {
        try {
            safeMap(element, context);
        } catch (Throwable ex) {
            LOGGER.error("Failed to process element", ex);
        }
    }

    private void safeMap(Element element, Context context) {
        if (element == null) {
            return;
        }
        context.setStatus("Element Id: " + element.getId());
        elementCache.add(element);
        if (elementCache.size() >= batchSize) {
            context.setStatus("Submitting batch: " + elementCache.size());
            writeCache();
        }
        context.getCounter(ReindexCounters.ELEMENTS_PROCESSED).increment(1);
    }

    private void writeCache() {
        if (elementCache.size() == 0) {
            return;
        }

        try {
            graph.getSearchIndex().addElements(graph, elementCache, authorizations);
        } catch (Throwable ex) {
            LOGGER.error("Could not add elements", ex);
        }
        elementCache.clear();
    }
}
