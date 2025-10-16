package org.openalex4j.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openalex4j.OpenAlexClient;
import org.openalex4j.Work;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        when(mockClient.searchWorks(eq("graph neural networks"), eq(3)))
                .thenReturn(List.of(work));
        Supplier<OpenAlexClient> supplier = () -> mockClient;
        OpenAlexCli cli = new OpenAlexCli(supplier, outStream, errStream);

        int exitCode = cli.run(new String[]{"--per-page", "3", "graph", "neural", "networks"});

        assertEquals(0, exitCode);
        String output = stdout.toString();
        assertTrue(output.contains("Top 1 result"), "Should print summary line");
        assertTrue(output.contains("Sample Work"), "Should print work display name");
        verify(mockClient).searchWorks("graph neural networks", 3);
    }
}
