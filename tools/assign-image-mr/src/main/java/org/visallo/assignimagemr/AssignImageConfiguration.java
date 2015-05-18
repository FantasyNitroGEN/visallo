package org.visallo.assignimagemr;

import org.apache.hadoop.conf.Configuration;
import org.vertexium.Visibility;
import org.vertexium.accumulo.AccumuloAuthorizations;
import org.vertexium.accumulo.mapreduce.VertexiumMRUtils;
import org.vertexium.util.ConvertingIterable;

import java.util.Map;

import static org.vertexium.util.IterableUtils.toArray;

public class AssignImageConfiguration {
    public static final String HAS_IMAGE_LABELS = "assignImageMR.hasImageLabel";
    private final AccumuloAuthorizations authorizations;
    private final String[] hasImageLabels;
    private final Visibility visibility;

    public AssignImageConfiguration(Configuration configuration) {
        this.authorizations = new AccumuloAuthorizations(configuration.getStrings(VertexiumMRUtils.CONFIG_AUTHORIZATIONS));
        Map<String, Map<String, String>> hasImageLabelsMap = org.visallo.core.config.Configuration.getMultiValue(configuration, HAS_IMAGE_LABELS);
        this.hasImageLabels = toArray(new ConvertingIterable<Map<String, String>, String>(hasImageLabelsMap.values()) {
            @Override
            protected String convert(Map<String, String> o) {
                return o.get("");
            }
        }, String.class);
        this.visibility = new Visibility("");
    }

    public AccumuloAuthorizations getAuthorizations() {
        return authorizations;
    }

    public String[] getHasImageLabels() {
        return hasImageLabels;
    }

    public Visibility getVisibility() {
        return visibility;
    }
}
