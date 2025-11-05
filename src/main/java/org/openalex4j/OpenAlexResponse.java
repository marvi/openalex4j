package org.openalex4j;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAlexResponse<T> {

    @JsonProperty("meta")
    private Meta meta;
    @JsonProperty("results")
    private List<T> results;
    @JsonProperty("group_by")
    private List<Map<String, Object>> groupBy;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        private int count;
        @JsonProperty("db_response_time_ms")
        private int dbResponseTimeMs;
        private int page;
        @JsonProperty("per_page")
        private int perPage;
        @JsonProperty("next_cursor")
        private String nextCursor;
    }
}
