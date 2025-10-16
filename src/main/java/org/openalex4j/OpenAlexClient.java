package org.openalex4j;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OpenAlexClient {

    private final static String API_BASE_URL = "https://api.openalex.org";
    private final static int DEFAULT_PAGE_SIZE = 200;

    private final Client client;
    private final ObjectMapper objectMapper;

    private OpenAlexClient(Client client, ObjectMapper objectMapper) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public static OpenAlexClient create() {
        Client client = ClientBuilder.newClient();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new OpenAlexClient(client, mapper);
    }

    static OpenAlexClient create(Client client, ObjectMapper objectMapper) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        return new OpenAlexClient(client, objectMapper);
    }

    public List<Work> searchWorks(String query) {
        return searchWorks(query, DEFAULT_PAGE_SIZE);
    }

    public List<Work> searchWorks(String query, int perPage) {
        String encodedQuery = encodeQuery(query);
        String url = API_BASE_URL + "/works?search=" + encodedQuery + "&per_page=" + perPage + "&sort=publication_date:desc";
        return executeSearch(url);
    }

    public List<Work> searchAllWorks(String query) {
        List<Work> allWorks = new ArrayList<>();
        String cursor = "*";
        String encodedQuery = encodeQuery(query);
        do {
            String url = API_BASE_URL + "/works?search=" + encodedQuery + "&per_page=" + DEFAULT_PAGE_SIZE + "&cursor=" + cursor + "&sort=publication_date:desc";
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
        try (Response response = client.target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get()) {

            int status = response.getStatus();
            String body = response.readEntity(String.class);
            if (status < 200 || status >= 300) {
                throw new OpenAlexException("OpenAlex API request failed with status " + status + " for URL " + url + ": " + body);
            }

            return objectMapper.readValue(body, new TypeReference<OpenAlexResponse<Work>>() {});
        } catch (ProcessingException e) {
            throw new OpenAlexException("Error executing search request to OpenAlex API", e);
        } catch (IOException e) {
            throw new OpenAlexException("Error parsing response from OpenAlex API", e);
        }
    }

    private String encodeQuery(String query) {
        return URLEncoder.encode(Objects.requireNonNull(query, "query must not be null"), StandardCharsets.UTF_8);
    }
}
