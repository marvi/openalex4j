package org.openalex4j;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

    private OpenAlexClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this(httpClient, objectMapper, DEFAULT_LANGUAGES, List.of(), SearchMode.BROAD);
    }

    private OpenAlexClient(HttpClient httpClient, ObjectMapper objectMapper, List<String> languages) {
        this(httpClient, objectMapper, languages, List.of(), SearchMode.BROAD);
    }

    private OpenAlexClient(HttpClient httpClient, ObjectMapper objectMapper, List<String> languages, List<String> concepts,
                           SearchMode searchMode) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.allowedLanguages = List.copyOf(normalizeLanguages(languages));
        this.conceptFilters = List.copyOf(normalizeConcepts(concepts));
        this.searchMode = Objects.requireNonNullElse(searchMode, SearchMode.BROAD);
    }

    public static OpenAlexClient create() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new OpenAlexClient(client, mapper, DEFAULT_LANGUAGES, List.of(), SearchMode.BROAD);
    }

    static OpenAlexClient create(HttpClient client, ObjectMapper objectMapper) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        return new OpenAlexClient(client, objectMapper, DEFAULT_LANGUAGES, List.of(), SearchMode.BROAD);
    }

    public OpenAlexClient withAllowedLanguages(List<String> languages) {
        return new OpenAlexClient(httpClient, objectMapper, languages, conceptFilters, searchMode);
    }

    public OpenAlexClient withConcepts(List<String> concepts) {
        return new OpenAlexClient(httpClient, objectMapper, allowedLanguages, concepts, searchMode);
    }

    public OpenAlexClient withSearchMode(SearchMode mode) {
        return new OpenAlexClient(httpClient, objectMapper, allowedLanguages, conceptFilters, mode);
    }

    public List<Work> searchWorks(String query) {
        return searchWorks(query, DEFAULT_PAGE_SIZE);
    }

    public List<Work> searchWorks(String query, int perPage) {
        String url = API_BASE_URL + "/works?" + buildSearchQuery(query)
                + "&per_page=" + perPage + buildFilterQuery() + "&sort=publication_date:desc";
        return executeSearch(url);
    }

    public List<Work> searchAllWorks(String query) {
        List<Work> allWorks = new ArrayList<>();
        String cursor = "*";
        do {
            String url = API_BASE_URL + "/works?" + buildSearchQuery(query)
                    + "&per_page=" + DEFAULT_PAGE_SIZE
                    + "&cursor=" + cursor + buildFilterQuery() + "&sort=publication_date:desc";
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

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body();

            if (status < 200 || status >= 300) {
                throw new OpenAlexException("OpenAlex API request failed with status " + status + " for URL " + url + ": " + body);
            }

            return objectMapper.readValue(body, new TypeReference<OpenAlexResponse<Work>>() {});
        } catch (IOException e) {
            throw new OpenAlexException("Error executing search request to OpenAlex API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
        String expression = switch (searchMode) {
            case TITLE_ONLY -> "title:\"" + trimmed + "\"";
            case ABSTRACT_ONLY -> "abstract:\"" + trimmed + "\"";
            case TITLE_AND_ABSTRACT -> "title:\"" + trimmed + "\" OR abstract:\"" + trimmed + "\"";
            case BROAD -> trimmed;
        };
        return "search=" + encodeQuery(expression);
    }

    private String buildFilterQuery() {
        List<String> filters = new ArrayList<>();
        if (!allowedLanguages.isEmpty()) {
            filters.add("language:" + String.join("|", allowedLanguages));
        }
        if (!conceptFilters.isEmpty()) {
            filters.add("concept.id:" + String.join("|", conceptFilters));
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
