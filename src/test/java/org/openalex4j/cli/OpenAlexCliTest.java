package org.openalex4j.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openalex4j.Authorship;
import org.openalex4j.OpenAlexClient;
import org.openalex4j.Work;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenAlexCliTest {

    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;
    private PrintStream outStream;
    private PrintStream errStream;

    @BeforeEach
    void setUp() {
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        outStream = new PrintStream(stdout);
        errStream = new PrintStream(stderr);
    }

    @Test
    void runWithoutArgumentsPrintsUsage() {
        OpenAlexCli cli = new OpenAlexCli(OpenAlexClient::create, outStream, errStream);

        int exitCode = cli.run(new String[]{});

        assertEquals(1, exitCode);
        assertTrue(stderr.toString().contains("Usage"), "Usage text should be printed");
    }

    @Test
    void runWithInvalidPerPagePrintsError() {
        OpenAlexCli cli = new OpenAlexCli(OpenAlexClient::create, outStream, errStream);

        int exitCode = cli.run(new String[]{"--per-page", "-1", "quantum"});

        assertEquals(1, exitCode);
        assertTrue(stderr.toString().contains("Per page value must be between 1 and 200"));
    }

    @Test
    void runWithValidQueryPrintsResults() {
        OpenAlexClient mockClient = Mockito.mock(OpenAlexClient.class);
        Work work = new Work();
        work.setId("https://openalex.org/W123");
        work.setDisplayName("Sample Work");
        work.setPublicationYear(2024);
        work.setType("article");
        work.setCitedByCount(42);
        work.setPublicationDate(LocalDate.of(2024, 4, 28));
        work.setCreatedDate(LocalDate.of(2024, 4, 1));
        work.setUpdatedDate(LocalDate.of(2024, 5, 2));
        Work.Source source = new Work.Source();
        source.setDisplayName("Journal of Testing");
        Work.Location location = new Work.Location();
        location.setPdfUrl("https://example.org/fulltext.pdf");
        location.setSource(source);
        work.setPrimaryLocation(location);
        Work.Location best = new Work.Location();
        best.setPdfUrl("https://example.org/fulltext.pdf");
        work.setBestOALocation(best);
        Work.OpenAccess openAccess = new Work.OpenAccess();
        openAccess.setOa(true);
        openAccess.setOaStatus("gold");
        openAccess.setOaUrl("https://example.org/oa");
        work.setOpenAccess(openAccess);
        Authorship.Institution institution = new Authorship.Institution();
        institution.setDisplayName("Quantum Institute");
        institution.setCountryCode("SE");
        Authorship authorship = new Authorship();
        authorship.setInstitutions(List.of(institution));
        Work.Concept concept = new Work.Concept();
        concept.setDisplayName("Quantum Computing");
        work.setConcepts(List.of(concept));
        work.setAuthorships(List.of(authorship));
        when(mockClient.withSearchMode(Mockito.any())).thenReturn(mockClient);
        when(mockClient.searchWorks(eq("graph neural networks"), eq(3)))
                .thenReturn(List.of(work));
        Supplier<OpenAlexClient> supplier = () -> mockClient;
        OpenAlexCli cli = new OpenAlexCli(supplier, outStream, errStream);

        int exitCode = cli.run(new String[]{"--per-page", "3", "graph", "neural", "networks"});

        assertEquals(0, exitCode);
        String output = stdout.toString();
        assertTrue(output.contains("Top 1 result"), "Should print summary line");
        assertTrue(output.contains("Sample Work"), "Should print work display name");
        assertTrue(output.contains("Full text: https://example.org/fulltext.pdf"), "Should surface full text link");
        assertTrue(output.contains("Publication date: 2024-04-28"), "Should surface publication date");
        assertTrue(output.contains("Created: 2024-04-01"), "Should surface created date when available");
        assertTrue(output.contains("Updated: 2024-05-02"), "Should surface updated date when available");
        assertTrue(output.contains("Cited by: 42"), "Should surface citation count");
        assertTrue(output.contains("OA: gold"), "Should surface open access status");
        assertTrue(output.contains("Institutions: Quantum Institute (SE)"), "Should surface institution and country");
        assertTrue(output.contains("Concepts: Quantum Computing"), "Should surface concept information");
        verify(mockClient).withSearchMode(OpenAlexClient.SearchMode.BROAD);
        verify(mockClient).searchWorks("graph neural networks", 3);
    }

    @Test
    void runWithLanguagesOptionAppliesFilter() {
        OpenAlexClient mockClient = Mockito.mock(OpenAlexClient.class);
        when(mockClient.withAllowedLanguages(anyList())).thenReturn(mockClient);
        when(mockClient.withConcepts(anyList())).thenReturn(mockClient);
        when(mockClient.withSearchMode(Mockito.any())).thenReturn(mockClient);
        when(mockClient.searchWorks(eq("climate change"), eq(5))).thenReturn(List.of());
        Supplier<OpenAlexClient> supplier = () -> mockClient;
        OpenAlexCli cli = new OpenAlexCli(supplier, outStream, errStream);

        int exitCode = cli.run(new String[]{"--languages", "es, pt", "climate", "change"});

        assertEquals(0, exitCode);
        verify(mockClient).withAllowedLanguages(Arrays.asList("es", "pt"));
        verify(mockClient).withSearchMode(OpenAlexClient.SearchMode.BROAD);
        verify(mockClient).searchWorks("climate change", 5);
        assertTrue(stdout.toString().contains("No results found"), "Should report no results");
    }

    @Test
    void runWithConceptsOptionAppliesFilter() {
        OpenAlexClient mockClient = Mockito.mock(OpenAlexClient.class);
        when(mockClient.withConcepts(anyList())).thenReturn(mockClient);
        when(mockClient.withSearchMode(Mockito.any())).thenReturn(mockClient);
        when(mockClient.searchWorks(eq("religious studies"), eq(5))).thenReturn(List.of());
        Supplier<OpenAlexClient> supplier = () -> mockClient;
        OpenAlexCli cli = new OpenAlexCli(supplier, outStream, errStream);

        int exitCode = cli.run(new String[]{"--concepts", "C555206, C17744445", "religious", "studies"});

        assertEquals(0, exitCode);
        verify(mockClient).withConcepts(Arrays.asList("C555206", "C17744445"));
        verify(mockClient).withSearchMode(OpenAlexClient.SearchMode.BROAD);
        verify(mockClient).searchWorks("religious studies", 5);
        assertTrue(stdout.toString().contains("No results found"), "Should report no results");
    }

    @Test
    void runWithShowAbstractIncludesAbstract() {
        OpenAlexClient mockClient = Mockito.mock(OpenAlexClient.class);
        when(mockClient.withAllowedLanguages(anyList())).thenReturn(mockClient);
        when(mockClient.withConcepts(anyList())).thenReturn(mockClient);
        when(mockClient.withSearchMode(Mockito.any())).thenReturn(mockClient);
        Work work = new Work();
        work.setDisplayName("Abstract Test");
        work.setPublicationYear(2022);
        Map<String, List<Integer>> abstractIndex = new HashMap<>();
        abstractIndex.put("Sample", List.of(0));
        abstractIndex.put("abstract", List.of(1));
        abstractIndex.put("text", List.of(2));
        work.setAbstractInvertedIndex(abstractIndex);
        when(mockClient.searchWorks(eq("theology"), eq(5))).thenReturn(List.of(work));
        Supplier<OpenAlexClient> supplier = () -> mockClient;
        OpenAlexCli cli = new OpenAlexCli(supplier, outStream, errStream);

        int exitCode = cli.run(new String[]{"--show-abstract", "theology"});

        assertEquals(0, exitCode);
        String output = stdout.toString();
        assertTrue(output.contains("Abstract: Sample abstract text"), "Should include abstract when requested");
        verify(mockClient).withSearchMode(OpenAlexClient.SearchMode.BROAD);
        verify(mockClient).searchWorks("theology", 5);
    }

    @Test
    void runWithSearchModeTitleUsesTitleSearch() {
        OpenAlexClient mockClient = Mockito.mock(OpenAlexClient.class);
        when(mockClient.withAllowedLanguages(anyList())).thenReturn(mockClient);
        when(mockClient.withConcepts(anyList())).thenReturn(mockClient);
        when(mockClient.withSearchMode(Mockito.any())).thenReturn(mockClient);
        when(mockClient.searchWorks(eq("ritual violence"), eq(5))).thenReturn(List.of());
        Supplier<OpenAlexClient> supplier = () -> mockClient;
        OpenAlexCli cli = new OpenAlexCli(supplier, outStream, errStream);

        int exitCode = cli.run(new String[]{"--search-mode", "title", "ritual", "violence"});

        assertEquals(0, exitCode);
        verify(mockClient).withSearchMode(OpenAlexClient.SearchMode.TITLE_ONLY);
        verify(mockClient).searchWorks("ritual violence", 5);
    }

    @Test
    void runWithDateFiltersAppliesFilters() {
        OpenAlexClient mockClient = Mockito.mock(OpenAlexClient.class);
        when(mockClient.withCreatedSince(Mockito.anyInt())).thenReturn(mockClient);
        when(mockClient.withPublicationDateSince(Mockito.anyInt())).thenReturn(mockClient);
        when(mockClient.withSearchMode(Mockito.any())).thenReturn(mockClient);
        when(mockClient.searchWorks(eq("test"), eq(5))).thenReturn(List.of());
        Supplier<OpenAlexClient> supplier = () -> mockClient;
        OpenAlexCli cli = new OpenAlexCli(supplier, outStream, errStream);

        int exitCode = cli.run(new String[]{"--created-since", "10", "--published-since", "20", "test"});

        assertEquals(0, exitCode);
        verify(mockClient).withCreatedSince(10);
        verify(mockClient).withPublicationDateSince(20);
        verify(mockClient).searchWorks("test", 5);
    }
}
