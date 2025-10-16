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
    private Map<String, String> ids;
    private List<Author> authorships;
    private String doi;

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

    public Map<String, String> getIds() {
        return ids;
    }

    public void setIds(Map<String, String> ids) {
        this.ids = ids;
    }

    public List<Author> getAuthorships() {
        return authorships;
    }

    public void setAuthorships(List<Author> authorships) {
        this.authorships = authorships;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }
}
