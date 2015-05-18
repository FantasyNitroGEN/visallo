package org.visallo.core.status;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Counting;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import org.json.JSONObject;

import java.util.Collection;

public class MetricEntry {
    private final String name;
    private final Metric metric;

    public MetricEntry(String name, Metric metric) {
        this.name = name;
        this.metric = metric;
    }

    public String getName() {
        return name;
    }

    public Metric getMetric() {
        return metric;
    }

    public static JSONObject toJson(Collection<MetricEntry> metrics) {
        JSONObject json = new JSONObject();
        for (MetricEntry metric : metrics) {
            json.put(metric.getName(), toJson(metric.getMetric()));
        }
        return json;
    }

    public static JSONObject toJson(Metric metric) {
        if (metric instanceof Metered) {
            return toJson((Metered) metric);
        } else if (metric instanceof Counter) {
            return toJson((Counting) metric);
        } else {
            return new JSONObject();
        }
    }

    public static JSONObject toJson(Metered metered) {
        JSONObject json = new JSONObject();
        json.put("count", metered.getCount());
        json.put("oneMinuteRate", metered.getOneMinuteRate());
        json.put("fiveMinuteRate", metered.getFiveMinuteRate());
        json.put("fifteenMinuteRate", metered.getFifteenMinuteRate());
        json.put("meanRate", metered.getMeanRate());
        return json;
    }

    public static JSONObject toJson(Counting counting) {
        JSONObject json = new JSONObject();
        json.put("count", counting.getCount());
        return json;
    }
}
