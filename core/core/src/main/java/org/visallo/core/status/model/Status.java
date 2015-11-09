package org.visallo.core.status.model;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.visallo.core.exception.VisalloException;
import org.visallo.web.clientapi.model.ClientApiObject;

import java.util.Date;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ExternalResourceRunnerStatus.class, name = "externalResourceRunner"),
        @JsonSubTypes.Type(value = GraphPropertyRunnerStatus.class, name = "graphPropertyRunner"),
        @JsonSubTypes.Type(value = LongRunningProcessRunnerStatus.class, name = "longRunningProcessRunner"),
        @JsonSubTypes.Type(value = QueueStatus.class, name = "queue")
})
public abstract class Status implements ClientApiObject {
    private String className;
    private String name;
    private String description;
    private String projectVersion;
    private String gitRevision;
    private String builtBy;
    private Date builtOn;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }

    public String getGitRevision() {
        return gitRevision;
    }

    public void setGitRevision(String gitRevision) {
        this.gitRevision = gitRevision;
    }

    public String getBuiltBy() {
        return builtBy;
    }

    public void setBuiltBy(String builtBy) {
        this.builtBy = builtBy;
    }

    public Date getBuiltOn() {
        return builtOn;
    }

    public void setBuiltOn(Date builtOn) {
        this.builtOn = builtOn;
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = CounterMetric.class, name = "counter"),
            @JsonSubTypes.Type(value = TimerMetric.class, name = "timer")
    })
    public static abstract class Metric {
        public static Metric create(com.codahale.metrics.Metric metric) {
            if (metric instanceof Counter) {
                return new CounterMetric((Counter) metric);
            } else if (metric instanceof Timer) {
                return new TimerMetric((Timer) metric);
            }
            throw new VisalloException("Unhandled metric: " + metric.getClass().getName());
        }
    }

    @JsonTypeName("counter")
    public static class CounterMetric extends Metric {
        private long count;

        public CounterMetric() {

        }

        public CounterMetric(Counter counter) {
            this.count = counter.getCount();
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    @JsonTypeName("timer")
    public static class TimerMetric extends Metric {
        private long count;
        private double meanRate;
        private double oneMinuteRate;
        private double fiveMinuteRate;
        private double fifteenMinuteRate;

        public TimerMetric() {

        }

        public TimerMetric(Timer metric) {
            this.count = metric.getCount();
            this.meanRate = metric.getMeanRate();
            this.oneMinuteRate = metric.getOneMinuteRate();
            this.fiveMinuteRate = metric.getFiveMinuteRate();
            this.fifteenMinuteRate = metric.getFifteenMinuteRate();
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public double getMeanRate() {
            return meanRate;
        }

        public void setMeanRate(double meanRate) {
            this.meanRate = meanRate;
        }

        public double getOneMinuteRate() {
            return oneMinuteRate;
        }

        public void setOneMinuteRate(double oneMinuteRate) {
            this.oneMinuteRate = oneMinuteRate;
        }

        public double getFiveMinuteRate() {
            return fiveMinuteRate;
        }

        public void setFiveMinuteRate(double fiveMinuteRate) {
            this.fiveMinuteRate = fiveMinuteRate;
        }

        public double getFifteenMinuteRate() {
            return fifteenMinuteRate;
        }

        public void setFifteenMinuteRate(double fifteenMinuteRate) {
            this.fifteenMinuteRate = fifteenMinuteRate;
        }
    }
}
