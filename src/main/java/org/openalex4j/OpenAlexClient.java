package org.openalex4j;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OpenAlexClient {

    private final static String API_BASE_URL = "https://api.openalex.org";
    private final static int DEFAULT_PAGE_SIZE = 200;

    private final static List<String> DEFAULT_LANGUAGES = List.of("sv", "da", "no", "de", "fr", "en");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final List<String> allowedLanguages;
    private final List<String> conceptFilters;
    private final SearchMode searchMode;
    private final LocalDate fromCreatedDate;
    private final LocalDate fromPublicationDate;
    private static final Logger LOG = LoggerFactory.getLogger(OpenAlexClient.class);

    private OpenAlexClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this(httpClient, objectMapper, DEFAULT_LANGUAGES, List.of(), SearchMode.BROAD, null, null);
    }

    private OpenAlexClient(HttpClient httpClient, ObjectMapper objectMapper, List<String> languages) {
        this(httpClient, objectMapper, languages, List.of(), SearchMode.BROAD, null, null);
    }

    private OpenAlexClient(HttpClient httpClient, ObjectMapper objectMapper, List<String> languages, List<String> concepts,
                           SearchMode searchMode) {
        this(httpClient, objectMapper, languages, concepts, searchMode, null, null);
    }

    private OpenAlexClient(HttpClient httpClient, ObjectMapper objectMapper, List<String> languages, List<String> concepts,
                           SearchMode searchMode, LocalDate fromCreatedDate, LocalDate fromPublicationDate) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.allowedLanguages = List.copyOf(normalizeLanguages(languages));
        this.conceptFilters = List.copyOf(normalizeConcepts(concepts));
        this.searchMode = Objects.requireNonNullElse(searchMode, SearchMode.BROAD);
        this.fromCreatedDate = fromCreatedDate;
        this.fromPublicationDate = fromPublicationDate;
    }

    public static OpenAlexClient create() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new OpenAlexClient(client, mapper, DEFAULT_LANGUAGES, List.of(), SearchMode.BROAD, null, null);
    }

    static OpenAlexClient create(HttpClient client, ObjectMapper objectMapper) {
        objectMapper.registerModule(new JavaTimeModule());
        return new OpenAlexClient(client, objectMapper, DEFAULT_LANGUAGES, List.of(), SearchMode.BROAD, null, null);
    }

    public OpenAlexClient withAllowedLanguages(List<String> languages) {
        return new OpenAlexClient(httpClient, objectMapper, languages, conceptFilters, searchMode, fromCreatedDate, fromPublicationDate);
    }

    public OpenAlexClient withConcepts(List<String> concepts) {
        return new OpenAlexClient(httpClient, objectMapper, allowedLanguages, concepts, searchMode, fromCreatedDate, fromPublicationDate);
    }

    public OpenAlexClient withSearchMode(SearchMode mode) {
        return new OpenAlexClient(httpClient, objectMapper, allowedLanguages, conceptFilters, mode, fromCreatedDate, fromPublicationDate);
    }

    public OpenAlexClient withFromCreatedDate(LocalDate fromCreatedDate) {
        return new OpenAlexClient(httpClient, objectMapper, allowedLanguages, conceptFilters, searchMode, fromCreatedDate, fromPublicationDate);
    }

    public OpenAlexClient withFromPublicationDate(LocalDate fromPublicationDate) {
        return new OpenAlexClient(httpClient, objectMapper, allowedLanguages, conceptFilters, searchMode, fromCreatedDate, fromPublicationDate);
    }

    public OpenAlexClient withCreatedSince(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be a positive number.");
        }
        LocalDate fromDate = LocalDate.now().minusDays(days);
        return new OpenAlexClient(httpClient, objectMapper, allowedLanguages, conceptFilters, searchMode, fromDate, fromPublicationDate);
    }

    public OpenAlexClient withPublicationDateSince(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be a positive number.");
        }
        LocalDate fromDate = LocalDate.now().minusDays(days);
        return new OpenAlexClient(httpClient, objectMapper, allowedLanguages, conceptFilters, searchMode, fromCreatedDate, fromDate);
    }

    public List<Work> searchWorks(String query) {
        return searchWorks(query, DEFAULT_PAGE_SIZE);
    }

    public List<Work> searchWorks(String query, int perPage) {
        String trimmed = Objects.requireNonNull(query, "query must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }
        String searchQuery = buildSearchQuery(trimmed);
        String url = API_BASE_URL + "/works?" + searchQuery
                + (searchQuery.isEmpty() ? "" : "&") + "per_page=" + perPage + buildFilterQuery(trimmed) + "&sort=publication_date:desc";
        return executeSearch(url);
    }

    public List<Work> searchAllWorks(String query) {
        List<Work> allWorks = new ArrayList<>();
        String cursor = "*";
        String trimmed = Objects.requireNonNull(query, "query must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }
        String searchQuery = buildSearchQuery(trimmed);
        do {
            String url = API_BASE_URL + "/works?" + searchQuery
                    + (searchQuery.isEmpty() ? "" : "&") + "per_page=" + DEFAULT_PAGE_SIZE
                    + "&cursor=" + cursor + buildFilterQuery(trimmed) + "&sort=publication_date:desc";
            OpenAlexResponse<Work> response = executeSearchAndGetResponse(url);
            if (response.getResults() != null) {
                allWorks.addAll(response.getResults());
            }
            String nextCursor = response.getMeta() != null ? response.getMeta().getNextCursor() : null;
            if (nextCursor == null || nextCursor.isEmpty() || nextCursor.equals(cursor)) {
                cursor = null;
            } else {
                cursor = nextCursor;
            }
        } while (cursor != null);
        return allWorks;
    }

    private List<Work> executeSearch(String url) {
        OpenAlexResponse<Work> response = executeSearchAndGetResponse(url);
        return response.getResults() != null ? response.getResults() : new ArrayList<>();
    }

    private OpenAlexResponse<Work> executeSearchAndGetResponse(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("User-Agent", "openalex4j/1.0 (Java HttpClient)")
                .GET()
                .build();
        LOG.debug("OpenAlex request: {}", url);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body();

            if (status < 200 || status >= 300) {
                LOG.warn("OpenAlex response {} for URL {}", status, url);
                throw new OpenAlexException("OpenAlex API request failed with status " + status + " for URL " + url + ": " + body);
            }

            return objectMapper.readValue(body, new TypeReference<OpenAlexResponse<Work>>() {});
        } catch (IOException e) {
            LOG.error("I/O error calling OpenAlex", e);
            throw new OpenAlexException("Error executing search request to OpenAlex API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Interrupted while calling OpenAlex", e);
            throw new OpenAlexException("OpenAlex API request interrupted", e);
        }
    }

    private String encodeQuery(String query) {
        return URLEncoder.encode(Objects.requireNonNull(query, "query must not be null"), StandardCharsets.UTF_8);
    }

    private String buildSearchQuery(String query) {
        Objects.requireNonNull(query, "query must not be null");
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }
        if ("*".equals(trimmed)) {
            return "";
        }
        String expression = switch (searchMode) {
            case TITLE_ONLY -> "title:\"" + trimmed + "\"";
            case ABSTRACT_ONLY -> "abstract:\"" + trimmed + "\"";
            case TITLE_AND_ABSTRACT -> "title:\"" + trimmed + "\" OR abstract:\"" + trimmed + "\"";
            case BROAD -> trimmed;
        };
        return "search=" + encodeQuery(expression);
    }

    private String buildFilterQuery(String trimmedQuery) {
        List<String> filters = new ArrayList<>();
        if (!allowedLanguages.isEmpty()) {
            filters.add("language:" + String.join("|", allowedLanguages));
        }
        if (!conceptFilters.isEmpty()) {
            filters.add("concepts.id:" + String.join("|", conceptFilters));
        }
        if (fromCreatedDate != null) {
            filters.add("from_created_date:" + fromCreatedDate);
        }
        if (fromPublicationDate != null) {
            filters.add("from_publication_date:" + fromPublicationDate);
        }
        if (searchMode == SearchMode.TITLE_ONLY) {
            filters.add("title.search:" + trimmedQuery);
        } else if (searchMode == SearchMode.ABSTRACT_ONLY) {
            filters.add("abstract.search:" + trimmedQuery);
        }
        if (filters.isEmpty()) {
            return "";
        }
        String filterValue = String.join(",", filters);
        return "&filter=" + URLEncoder.encode(filterValue, StandardCharsets.UTF_8);
    }

    private List<String> normalizeLanguages(List<String> languages) {
        if (languages == null) {
            return DEFAULT_LANGUAGES;
        }
        List<String> sanitized = languages.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .toList();
        if (sanitized.isEmpty()) {
            return DEFAULT_LANGUAGES;
        }
        return sanitized;
    }

    private List<String> normalizeConcepts(List<String> concepts) {
        if (concepts == null) {
            return List.of();
        }
        List<String> sanitized = concepts.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        return sanitized;
    }

    public enum SearchMode {
        BROAD,
        TITLE_ONLY,
        ABSTRACT_ONLY,
        TITLE_AND_ABSTRACT
    }
}
