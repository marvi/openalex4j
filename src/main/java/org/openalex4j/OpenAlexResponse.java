package org.openalex4j;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class OpenAlexResponse<T> {

    @JsonProperty("meta")
    private Meta meta;
    @JsonProperty("results")
    private List<T> results;
    @JsonProperty("group_by")
    private List<Map<String, Object>> groupBy;

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public List<T> getResults() {
        return results;
    }

    public void setResults(List<T> results) {
        this.results = results;
    }

    public List<Map<String, Object>> getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(List<Map<String, Object>> groupBy) {
        this.groupBy = groupBy;
    }

    public static class Meta {
        private int count;
        @JsonProperty("db_response_time_ms")
        private int dbResponseTimeMs;
        private int page;
        @JsonProperty("per_page")
        private int perPage;
        @JsonProperty("next_cursor")
        private String nextCursor;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getDbResponseTimeMs() {
            return dbResponseTimeMs;
        }

        public void setDbResponseTimeMs(int dbResponseTimeMs) {
            this.dbResponseTimeMs = dbResponseTimeMs;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getPerPage() {
            return perPage;
        }

        public void setPerPage(int perPage) {
            this.perPage = perPage;
        }

        public String getNextCursor() {
            return nextCursor;
        }

        public void setNextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
        }
    }
}
