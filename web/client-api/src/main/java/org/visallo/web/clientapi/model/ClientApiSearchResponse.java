package org.visallo.web.clientapi.model;

import org.visallo.web.clientapi.util.ClientApiConverter;

import java.util.*;

public abstract class ClientApiSearchResponse implements ClientApiObject {
    private Integer nextOffset = null;
    private Long retrievalTime = null;
    private Long totalTime = null;
    private Long totalHits = null;
    private Long searchTime = null;
    private Map<String, AggregateResult> aggregates = new HashMap<>();

    public Integer getNextOffset() {
        return nextOffset;
    }

    public void setNextOffset(Integer nextOffset) {
        this.nextOffset = nextOffset;
    }

    public Long getRetrievalTime() {
        return retrievalTime;
    }

    public void setRetrievalTime(Long retrievalTime) {
        this.retrievalTime = retrievalTime;
    }

    public Long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(Long totalTime) {
        this.totalTime = totalTime;
    }

    public Long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(Long totalHits) {
        this.totalHits = totalHits;
    }

    public Long getSearchTime() {
        return searchTime;
    }

    public void setSearchTime(Long searchTime) {
        this.searchTime = searchTime;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }

    public abstract int getItemCount();

    public Map<String, AggregateResult> getAggregates() {
        return aggregates;
    }

    public abstract static class AggregateResult {

    }

    public static class TermsAggregateResult extends AggregateResult {
        private Map<String, Long> buckets = new HashMap<>();

        public Map<String, Long> getBuckets() {
            return buckets;
        }
    }

    public static class GeohashAggregateResult extends AggregateResult {
        private long maxCount;
        private Map<String, Bucket> buckets = new HashMap<>();

        public void setMaxCount(long maxCount) {
            this.maxCount = maxCount;
        }

        public long getMaxCount() {
            return maxCount;
        }

        public Map<String, Bucket> getBuckets() {
            return buckets;
        }

        public static class Bucket {
            private final ClientApiGeoRect cell;
            private final ClientApiGeoPoint point;
            private final long count;

            public Bucket(ClientApiGeoRect cell, ClientApiGeoPoint point, long count) {
                this.cell = cell;
                this.point = point;
                this.count = count;
            }

            public ClientApiGeoRect getCell() {
                return cell;
            }

            public ClientApiGeoPoint getPoint() {
                return point;
            }

            public long getCount() {
                return count;
            }
        }
    }

    public static class HistogramAggregateResult extends AggregateResult {
        private Map<String, Long> buckets = new HashMap<>();

        public Map<String, Long> getBuckets() {
            return buckets;
        }
    }

    public static class StatisticsAggregateResult extends AggregateResult {
        private long count;
        private double average;
        private double min;
        private double max;
        private double standardDeviation;
        private double sum;

        public void setCount(long count) {
            this.count = count;
        }

        public long getCount() {
            return count;
        }

        public void setAverage(double average) {
            this.average = average;
        }

        public double getAverage() {
            return average;
        }

        public void setMin(double min) {
            this.min = min;
        }

        public double getMin() {
            return min;
        }

        public void setMax(double max) {
            this.max = max;
        }

        public double getMax() {
            return max;
        }

        public void setStandardDeviation(double standardDeviation) {
            this.standardDeviation = standardDeviation;
        }

        public double getStandardDeviation() {
            return standardDeviation;
        }

        public void setSum(double sum) {
            this.sum = sum;
        }

        public double getSum() {
            return sum;
        }
    }

    public static ClientApiSearchResponse listToClientApiSearchResponse(List<List<Object>> rows) {
        ClientApiSearchResponse results;
        if (rows == null || rows.size() == 0) {
            results = new ClientApiVertexSearchResponse();
        } else if (rows.get(0).size() == 1 && rows.get(0).get(0) instanceof ClientApiVertex) {
            results = new ClientApiVertexSearchResponse();
            ((ClientApiVertexSearchResponse) results).getVertices().addAll(toClientApiVertex(rows));
        } else if (rows.get(0).size() == 1 && rows.get(0).get(0) instanceof ClientApiEdge) {
            results = new ClientApiEdgeSearchResponse();
            ((ClientApiEdgeSearchResponse) results).getResults().addAll(toClientApiEdge(rows));
        } else {
            results = new ClientApiScalarSearchResponse();
            ((ClientApiScalarSearchResponse) results).getResults().addAll(rows);
        }
        return results;
    }

    private static Collection<ClientApiVertex> toClientApiVertex(List<List<Object>> rows) {
        List<ClientApiVertex> results = new ArrayList<>();
        for (List<Object> row : rows) {
            results.add((ClientApiVertex) row.get(0));
        }
        return results;
    }

    private static Collection<ClientApiEdge> toClientApiEdge(List<List<Object>> rows) {
        List<ClientApiEdge> results = new ArrayList<>();
        for (List<Object> row : rows) {
            results.add((ClientApiEdge) row.get(0));
        }
        return results;
    }
}
