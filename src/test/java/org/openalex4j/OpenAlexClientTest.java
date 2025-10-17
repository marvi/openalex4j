package org.openalex4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.when;

public class OpenAlexClientTest {

    private OpenAlexClient openAlexClient;
    private HttpClient mockClient;

    @BeforeEach
    public void setUp() {
        mockClient = Mockito.mock(HttpClient.class);
        openAlexClient = OpenAlexClient.create(mockClient, new ObjectMapper());
    }

    @Test
    public void testSearchWorks() throws Exception {
        String jsonResponse = "{\"results\": [{\"id\": \"1\", \"display_name\": \"Test Work\"}]}";
        HttpResponse<String> response = mockHttpResponse(200, jsonResponse);
        when(mockClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(response);

        List<Work> works = openAlexClient.searchWorks("test");

        assertEquals(1, works.size());
        assertEquals("1", works.get(0).getId());
        assertEquals("Test Work", works.get(0).getDisplayName());
        assertDefaultLanguageFilterApplied();
    }

    @Test
    public void testSearchAllWorksHandlesPagination() throws Exception {
        String firstPage = """
                {
                  "meta": {"next_cursor": "abc"},
                  "results": [
                    {"id": "1", "display_name": "Work 1"}
                  ]
                }
                """;
        String secondPage = """
                {
                  "meta": {"next_cursor": null},
                  "results": [
                    {"id": "2", "display_name": "Work 2"}
                  ]
                }
                """;
        HttpResponse<String> firstResponse = mockHttpResponse(200, firstPage);
        HttpResponse<String> secondResponse = mockHttpResponse(200, secondPage);

        when(mockClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(firstResponse, secondResponse);

        List<Work> works = openAlexClient.searchAllWorks("test");

        assertEquals(2, works.size());
        assertEquals("1", works.get(0).getId());
        assertEquals("2", works.get(1).getId());
        assertDefaultLanguageFilterApplied();
    }

    @Test
    public void testSearchWorksThrowsOnHttpError() throws Exception {
        HttpResponse<String> response = mockHttpResponse(500, "{\"error\":\"Internal\"}");
        when(mockClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(response);

        assertThrows(OpenAlexException.class, () -> openAlexClient.searchWorks("test"));
        assertDefaultLanguageFilterApplied();
    }

    @Test
    public void testSearchWorksThrowsOnInvalidJson() throws Exception {
        HttpResponse<String> response = mockHttpResponse(200, "not-json");
        when(mockClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(response);

        assertThrows(OpenAlexException.class, () -> openAlexClient.searchWorks("test"));
        assertDefaultLanguageFilterApplied();
    }

    @Test
    public void testCustomLanguageFilter() throws Exception {
        OpenAlexClient customClient = openAlexClient.withAllowedLanguages(List.of("es", "pt"));
        HttpResponse<String> response = mockHttpResponse(200, "{\"results\": []}");
        when(mockClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(response);

        customClient.searchWorks("test");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        Mockito.verify(mockClient).send(captor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        String uri = captor.getValue().uri().toString();
        org.junit.jupiter.api.Assertions.assertTrue(uri.contains("filter=language%3Aes%7Cpt"),
                "Custom language filter should be applied: " + uri);
        Mockito.clearInvocations(mockClient);
    }

    @Test
    public void testConceptFilter() throws Exception {
        OpenAlexClient conceptClient = openAlexClient.withConcepts(List.of("C555206", "C17744445"));
        HttpResponse<String> response = mockHttpResponse(200, "{\"results\": []}");
        when(mockClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(response);

        conceptClient.searchWorks("test");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        Mockito.verify(mockClient).send(captor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        String uri = captor.getValue().uri().toString();
        org.junit.jupiter.api.Assertions.assertTrue(uri.contains("concept.id%3AC555206%7CC17744445"),
                "Concept filter should be applied: " + uri);
        Mockito.clearInvocations(mockClient);
    }

    @Test
    public void testSearchModeTitleOnly() throws Exception {
        OpenAlexClient titleClient = openAlexClient.withSearchMode(OpenAlexClient.SearchMode.TITLE_ONLY);
        HttpResponse<String> response = mockHttpResponse(200, "{\"results\": []}");
        when(mockClient.send(ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(response);

        titleClient.searchWorks("ritual violence");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        Mockito.verify(mockClient).send(captor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        String uri = captor.getValue().uri().toString();
        org.junit.jupiter.api.Assertions.assertTrue(uri.contains("search=title%3A%22ritual+violence%22"),
                "Title-only search should be used: " + uri);
        Mockito.clearInvocations(mockClient);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockHttpResponse(int statusCode, String body) {
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }

    private void assertDefaultLanguageFilterApplied() throws Exception {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        Mockito.verify(mockClient, atLeastOnce()).send(captor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        for (HttpRequest request : captor.getAllValues()) {
            String uri = request.uri().toString();
            org.junit.jupiter.api.Assertions.assertTrue(
                    uri.contains("filter=language%3Asv%7Cda%7Cno%7Cde%7Cfr%7Cen"),
                    "Default language filter should be present in request: " + uri);
        }
        Mockito.clearInvocations(mockClient);
    }
}
