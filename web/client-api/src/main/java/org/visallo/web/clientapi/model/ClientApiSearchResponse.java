package org.visallo.web.clientapi.model;

import org.visallo.web.clientapi.util.ClientApiConverter;

import java.util.HashMap;
import java.util.Map;

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

    public static class HistographAggregateResult extends AggregateResult {
        private Map<String, Long> buckets = new HashMap<>();

        public Map<String, Long> getBuckets() {
            return buckets;
        }
    }
}
