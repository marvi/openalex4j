package org.openalex4j;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class Work {

    private String id;
    @JsonProperty("display_name")
    private String displayName;
    private String title;
    @JsonProperty("publication_year")
    private int publicationYear;
    @JsonProperty("publication_date")
    private LocalDate publicationDate;
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

    // Getters and setters for all fields

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public int getCitedByCount() {
        return citedByCount;
    }

    public void setCitedByCount(int citedByCount) {
        this.citedByCount = citedByCount;
    }

    public Map<String, String> getIds() {
        return ids;
    }

    public void setIds(Map<String, String> ids) {
        this.ids = ids;
    }

    public List<Authorship> getAuthorships() {
        return authorships;
    }

    public void setAuthorships(List<Authorship> authorships) {
        this.authorships = authorships;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public Location getPrimaryLocation() {
        return primaryLocation;
    }

    public void setPrimaryLocation(Location primaryLocation) {
        this.primaryLocation = primaryLocation;
    }

    public Location getBestOALocation() {
        return bestOALocation;
    }

    public void setBestOALocation(Location bestOALocation) {
        this.bestOALocation = bestOALocation;
    }

    public OpenAccess getOpenAccess() {
        return openAccess;
    }

    public void setOpenAccess(OpenAccess openAccess) {
        this.openAccess = openAccess;
    }

    public List<Concept> getConcepts() {
        return concepts;
    }

    public void setConcepts(List<Concept> concepts) {
        this.concepts = concepts;
    }

    public Map<String, List<Integer>> getAbstractInvertedIndex() {
        return abstractInvertedIndex;
    }

    public void setAbstractInvertedIndex(Map<String, List<Integer>> abstractInvertedIndex) {
        this.abstractInvertedIndex = abstractInvertedIndex;
    }

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

    public static class Location {
        @JsonProperty("landing_page_url")
        private String landingPageUrl;
        @JsonProperty("pdf_url")
        private String pdfUrl;
        private Source source;

        public String getLandingPageUrl() {
            return landingPageUrl;
        }

        public void setLandingPageUrl(String landingPageUrl) {
            this.landingPageUrl = landingPageUrl;
        }

        public String getPdfUrl() {
            return pdfUrl;
        }

        public void setPdfUrl(String pdfUrl) {
            this.pdfUrl = pdfUrl;
        }

        public Source getSource() {
            return source;
        }

        public void setSource(Source source) {
            this.source = source;
        }
    }

    public static class Source {
        private String id;
        @JsonProperty("display_name")
        private String displayName;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

    public static class OpenAccess {
        @JsonProperty("is_oa")
        private boolean isOa;
        @JsonProperty("oa_status")
        private String oaStatus;
        @JsonProperty("oa_url")
        private String oaUrl;

        public boolean isOa() {
            return isOa;
        }

        public void setOa(boolean oa) {
            isOa = oa;
        }

        public String getOaStatus() {
            return oaStatus;
        }

        public void setOaStatus(String oaStatus) {
            this.oaStatus = oaStatus;
        }

        public String getOaUrl() {
            return oaUrl;
        }

        public void setOaUrl(String oaUrl) {
            this.oaUrl = oaUrl;
        }
    }

    public static class Concept {
        private String id;
        @JsonProperty("display_name")
        private String displayName;
        private int level;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }
    }
}
