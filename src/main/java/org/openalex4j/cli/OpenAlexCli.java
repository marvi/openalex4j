package org.openalex4j.cli;

import org.openalex4j.OpenAlexClient;
import org.openalex4j.OpenAlexException;
import org.openalex4j.Work;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Minimal CLI for querying the OpenAlex API.
 * The CLI is intentionally lightweight – pass the keyword(s) as arguments and receive a formatted list of works.
 */
public class OpenAlexCli {

    private static final int DEFAULT_PAGE_SIZE = 5;

    private final Supplier<OpenAlexClient> clientSupplier;
    private final PrintStream out;
    private final PrintStream err;

    public OpenAlexCli() {
        this(OpenAlexClient::create, System.out, System.err);
    }

    OpenAlexCli(Supplier<OpenAlexClient> clientSupplier, PrintStream out, PrintStream err) {
        this.clientSupplier = Objects.requireNonNull(clientSupplier, "clientSupplier must not be null");
        this.out = Objects.requireNonNull(out, "out must not be null");
        this.err = Objects.requireNonNull(err, "err must not be null");
    }

    public static void main(String[] args) {
        OpenAlexCli cli = new OpenAlexCli();
        int exitCode = cli.run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    int run(String[] args) {
        ParsedInput parsed = parseArguments(args);
        if (!parsed.valid) {
            printUsage(err);
            return 1;
        }

        String query = parsed.query;
        int perPage = parsed.perPage;

        try {
            OpenAlexClient client = clientSupplier.get();
            List<Work> works = client.searchWorks(query, perPage);
            if (works.isEmpty()) {
                out.printf("No results found for \"%s\".%n", query);
                return 0;
            }

            out.printf("Top %d result%s for \"%s\":%n", works.size(), works.size() == 1 ? "" : "s", query);
            for (int i = 0; i < works.size(); i++) {
                Work work = works.get(i);
                out.printf("%d. %s%n", i + 1, formatWork(work));
            }
            return 0;
        } catch (OpenAlexException e) {
            err.printf("OpenAlex request failed: %s%n", e.getMessage());
            return 2;
        } catch (RuntimeException e) {
            err.printf("Unexpected error: %s%n", e.getMessage());
            return 2;
        }
    }

    private ParsedInput parseArguments(String[] args) {
        if (args == null || args.length == 0) {
            return ParsedInput.invalid();
        }

        int perPage = DEFAULT_PAGE_SIZE;
        List<String> queryParts = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--per-page".equals(arg) || "-n".equals(arg)) {
                if (i + 1 >= args.length) {
                    err.println("Missing value for --per-page option.");
                    return ParsedInput.invalid();
                }
                String value = args[++i];
                try {
                    perPage = Integer.parseInt(value);
                    if (perPage <= 0 || perPage > 200) {
                        err.println("Per page value must be between 1 and 200.");
                        return ParsedInput.invalid();
                    }
                } catch (NumberFormatException ex) {
                    err.printf("Invalid per page value: %s%n", value);
                    return ParsedInput.invalid();
                }
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                return ParsedInput.invalid();
            } else {
                queryParts.add(arg);
            }
        }

        if (queryParts.isEmpty()) {
            err.println("Please provide a search query.");
            return ParsedInput.invalid();
        }

        String query = String.join(" ", queryParts);
        return ParsedInput.valid(query, perPage);
    }

    private String formatWork(Work work) {
        String name = firstNonBlank(work.getDisplayName(), work.getTitle(), work.getId());
        String year = work.getPublicationYear() > 0 ? String.valueOf(work.getPublicationYear()) : "Unknown Year";
        String doi = work.getDoi() != null ? work.getDoi() : "";
        if (!doi.isBlank()) {
            return String.format("%s (%s) - %s", name, year, doi);
        }
        return String.format("%s (%s)", name, year);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private void printUsage(PrintStream stream) {
        stream.println("Usage: openalex-cli [--per-page <1-200>] <search terms>");
        stream.println("Examples:");
        stream.println("  openalex-cli quantum computing");
        stream.println("  openalex-cli --per-page 3 \"graph neural networks\"");
    }

    private static class ParsedInput {
        private final boolean valid;
        private final String query;
        private final int perPage;

        private ParsedInput(boolean valid, String query, int perPage) {
            this.valid = valid;
            this.query = query;
            this.perPage = perPage;
        }

        static ParsedInput invalid() {
            return new ParsedInput(false, null, DEFAULT_PAGE_SIZE);
        }

        static ParsedInput valid(String query, int perPage) {
            return new ParsedInput(true, query, perPage);
        }
    }
}
