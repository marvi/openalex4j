package org.openalex4j;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Author {
    @JsonProperty("author")
    private Map<String, String> authorInfo;
    private String rawAffiliationString;

    public Map<String, String> getAuthorInfo() {
        return authorInfo;
    }

    public void setAuthorInfo(Map<String, String> authorInfo) {
        this.authorInfo = authorInfo;
    }

    public String getRawAffiliationString() {
        return rawAffiliationString;
    }

    public void setRawAffiliationString(String rawAffiliationString) {
        this.rawAffiliationString = rawAffiliationString;
    }
}
