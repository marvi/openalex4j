package org.openalex4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Executes live requests against the OpenAlex API.
 * Disabled by default; set OPENALEX_LIVE_TEST=true to run.
 */
@EnabledIfEnvironmentVariable(named = "OPENALEX_LIVE_TEST", matches = "(?i)true")
class OpenAlexClientIntegrationTest {

    private static final String PAGINATED_QUERY = "\"Graph neural networks for traffic forecasting\"";

    @Test
    void searchWorksLiveReturnsResults() {
        OpenAlexClient client = OpenAlexClient.create();

        List<Work> works = client.searchWorks("machine learning", 5);

        assertFalse(works.isEmpty(), "Expected non-empty results from live OpenAlex API");
        Work first = works.get(0);
        assertNotNull(first.getId(), "Work should have an id");
        assertNotNull(first.getDisplayName(), "Work should have a display name");
    }

    @Test
    void searchAllWorksLiveAggregatesPages() {
        OpenAlexClient client = OpenAlexClient.create();

        List<Work> works = client.searchAllWorks(PAGINATED_QUERY);

        assertFalse(works.isEmpty(), "Expected at least one work for phrase search");
        works.forEach(work -> assertNotNull(work.getId(), "Work should have an id"));
    }
}
