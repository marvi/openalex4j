package org.openalex4j;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Work {

    private String id;
    @JsonProperty("display_name")
    private String displayName;
    private String title;
    @JsonProperty("publication_year")
    private int publicationYear;
    @JsonProperty("publication_date")
    private LocalDate publicationDate;
    @JsonProperty("created_date")
    private LocalDate createdDate;
    @JsonProperty("updated_date")
    private LocalDate updatedDate;
    private String type;
    @JsonProperty("relevance_score")
    private double relevanceScore;
    @JsonProperty("cited_by_count")
    private int citedByCount;
    private Map<String, String> ids;
    private List<Authorship> authorships;
    private String doi;
    @JsonProperty("primary_location")
    private Location primaryLocation;
    @JsonProperty("best_oa_location")
    private Location bestOALocation;
    @JsonProperty("open_access")
    private OpenAccess openAccess;
    private List<Concept> concepts;
    @JsonProperty("abstract_inverted_index")
    private Map<String, List<Integer>> abstractInvertedIndex;

    public String getAbstractText() {
        if (abstractInvertedIndex == null || abstractInvertedIndex.isEmpty()) {
            return "";
        }
        int length = abstractInvertedIndex.values().stream()
                .flatMap(List::stream)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(-1) + 1;
        if (length <= 0) {
            return "";
        }
        String[] tokens = new String[length];
        abstractInvertedIndex.forEach((word, positions) -> {
            for (Integer position : positions) {
                if (position >= 0 && position < tokens.length) {
                    tokens[position] = word;
                }
            }
        });
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token == null) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(token);
        }
        return builder.toString();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        @JsonProperty("landing_page_url")
        private String landingPageUrl;
        @JsonProperty("pdf_url")
        private String pdfUrl;
        private Source source;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Source {
        private String id;
        @JsonProperty("display_name")
        private String displayName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenAccess {
        @JsonProperty("is_oa")
        private boolean isOa;
        @JsonProperty("oa_status")
        private String oaStatus;
        @JsonProperty("oa_url")
        private String oaUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Concept {
        private String id;
        @JsonProperty("display_name")
        private String displayName;
        private int level;
    }
}
