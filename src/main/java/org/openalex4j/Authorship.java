package org.openalex4j;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Authorship {

    private Contributor author;
    private List<Institution> institutions;
    private List<String> countries;
    @JsonProperty("raw_author_name")
    private String rawAuthorName;
    @JsonProperty("author_position")
    private String authorPosition;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contributor {
        private String id;
        @JsonProperty("display_name")
        private String displayName;
        private String orcid;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Institution {
        private String id;
        @JsonProperty("display_name")
        private String displayName;
        @JsonProperty("country_code")
        private String countryCode;
        private String type;
    }
}
