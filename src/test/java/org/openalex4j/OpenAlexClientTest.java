package org.openalex4j;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class OpenAlexClientTest {

    private OpenAlexClient openAlexClient;
    private Client mockClient;
    private WebTarget mockWebTarget;
    private Invocation.Builder mockBuilder;

    @BeforeEach
    public void setUp() {
        mockClient = Mockito.mock(Client.class);
        mockWebTarget = Mockito.mock(WebTarget.class);
        mockBuilder = Mockito.mock(Invocation.Builder.class);

        when(mockClient.target(anyString())).thenReturn(mockWebTarget);
        when(mockWebTarget.request()).thenReturn(mockBuilder);
        when(mockBuilder.accept(MediaType.APPLICATION_JSON_TYPE)).thenReturn(mockBuilder);

        openAlexClient = OpenAlexClient.create(mockClient, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    public void testSearchWorks() {
        String jsonResponse = "{\"results\": [{\"id\": \"1\", \"display_name\": \"Test Work\"}]}";
        jakarta.ws.rs.core.Response response = Mockito.mock(jakarta.ws.rs.core.Response.class);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(eq(String.class))).thenReturn(jsonResponse);
        when(mockBuilder.get()).thenReturn(response);

        List<Work> works = openAlexClient.searchWorks("test");

        assertEquals(1, works.size());
        assertEquals("1", works.get(0).getId());
        assertEquals("Test Work", works.get(0).getDisplayName());
    }

    @Test
    public void testSearchAllWorksHandlesPagination() {
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
        jakarta.ws.rs.core.Response firstResponse = Mockito.mock(jakarta.ws.rs.core.Response.class);
        jakarta.ws.rs.core.Response secondResponse = Mockito.mock(jakarta.ws.rs.core.Response.class);
        when(firstResponse.getStatus()).thenReturn(200);
        when(secondResponse.getStatus()).thenReturn(200);
        when(firstResponse.readEntity(eq(String.class))).thenReturn(firstPage);
        when(secondResponse.readEntity(eq(String.class))).thenReturn(secondPage);

        when(mockBuilder.get()).thenReturn(firstResponse, secondResponse);

        List<Work> works = openAlexClient.searchAllWorks("test");

        assertEquals(2, works.size());
        assertEquals("1", works.get(0).getId());
        assertEquals("2", works.get(1).getId());
    }

    @Test
    public void testSearchWorksThrowsOnHttpError() {
        jakarta.ws.rs.core.Response errorResponse = Mockito.mock(jakarta.ws.rs.core.Response.class);
        when(errorResponse.getStatus()).thenReturn(500);
        when(errorResponse.readEntity(eq(String.class))).thenReturn("{\"error\":\"Internal\"}");
        when(mockBuilder.get()).thenReturn(errorResponse);

        assertThrows(OpenAlexException.class, () -> openAlexClient.searchWorks("test"));
    }

    @Test
    public void testSearchWorksThrowsOnInvalidJson() {
        jakarta.ws.rs.core.Response invalidResponse = Mockito.mock(jakarta.ws.rs.core.Response.class);
        when(invalidResponse.getStatus()).thenReturn(200);
        when(invalidResponse.readEntity(eq(String.class))).thenReturn("not-json");
        when(mockBuilder.get()).thenReturn(invalidResponse);

        assertThrows(OpenAlexException.class, () -> openAlexClient.searchWorks("test"));
    }
}
