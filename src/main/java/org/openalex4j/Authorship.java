package org.openalex4j;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Authorship {

    private Contributor author;
    private List<Institution> institutions;
    private List<String> countries;
    @JsonProperty("raw_author_name")
    private String rawAuthorName;
    @JsonProperty("author_position")
    private String authorPosition;

    public Contributor getAuthor() {
        return author;
    }

    public void setAuthor(Contributor author) {
        this.author = author;
    }

    public List<Institution> getInstitutions() {
        return institutions;
    }

    public void setInstitutions(List<Institution> institutions) {
        this.institutions = institutions;
    }

    public List<String> getCountries() {
        return countries;
    }

    public void setCountries(List<String> countries) {
        this.countries = countries;
    }

    public String getRawAuthorName() {
        return rawAuthorName;
    }

    public void setRawAuthorName(String rawAuthorName) {
        this.rawAuthorName = rawAuthorName;
    }

    public String getAuthorPosition() {
        return authorPosition;
    }

    public void setAuthorPosition(String authorPosition) {
        this.authorPosition = authorPosition;
    }

    public static class Contributor {
        private String id;
        @JsonProperty("display_name")
        private String displayName;
        private String orcid;

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

        public String getOrcid() {
            return orcid;
        }

        public void setOrcid(String orcid) {
            this.orcid = orcid;
        }
    }

    public static class Institution {
        private String id;
        @JsonProperty("display_name")
        private String displayName;
        @JsonProperty("country_code")
        private String countryCode;
        private String type;

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

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
