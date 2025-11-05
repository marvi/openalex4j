package org.openalex4j.cli;

import org.openalex4j.Authorship;
import org.openalex4j.OpenAlexClient;
import org.openalex4j.OpenAlexException;
import org.openalex4j.Work;

import java.io.PrintStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
            if (parsed.languages != null) {
                client = client.withAllowedLanguages(parsed.languages);
            }
            if (parsed.concepts != null) {
                client = client.withConcepts(parsed.concepts);
            }
            if (parsed.createdSince != null) {
                client = client.withCreatedSince(parsed.createdSince);
            }
            if (parsed.publishedSince != null) {
                client = client.withPublicationDateSince(parsed.publishedSince);
            }
            client = client.withSearchMode(parsed.searchMode);
            List<Work> works = client.searchWorks(query, perPage);
            if (works.isEmpty()) {
                out.printf("No results found for \"%s\".%n", query);
                return 0;
            }

            out.printf("Top %d result%s for \"%s\":%n", works.size(), works.size() == 1 ? "" : "s", query);
            for (int i = 0; i < works.size(); i++) {
                Work work = works.get(i);
                out.print(formatWork(work, parsed.showAbstract, i + 1));
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
        List<String> languages = null;
        List<String> concepts = null;
        Integer createdSince = null;
        Integer publishedSince = null;
        boolean showAbstract = false;
        OpenAlexClient.SearchMode searchMode = OpenAlexClient.SearchMode.BROAD;

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
            } else if ("--languages".equals(arg) || "-l".equals(arg)) {
                if (i + 1 >= args.length) {
                    err.println("Missing value for --languages option.");
                    return ParsedInput.invalid();
                }
                String value = args[++i];
                List<String> parsedLanguages = parseLanguages(value);
                if (parsedLanguages.isEmpty()) {
                    err.println("No valid languages provided. Use comma-separated language codes (e.g. en,sv,de).");
                    return ParsedInput.invalid();
                }
                languages = parsedLanguages;
            } else if ("--concepts".equals(arg) || "-c".equals(arg)) {
                if (i + 1 >= args.length) {
                    err.println("Missing value for --concepts option.");
                    return ParsedInput.invalid();
                }
                String value = args[++i];
                List<String> parsedConcepts = parseConcepts(value);
                if (parsedConcepts.isEmpty()) {
                    err.println("No valid concepts provided. Use comma-separated OpenAlex concept IDs.");
                    return ParsedInput.invalid();
                }
                concepts = parsedConcepts;
            } else if ("--created-since".equals(arg)) {
                if (i + 1 >= args.length) {
                    err.println("Missing value for --created-since option.");
                    return ParsedInput.invalid();
                }
                String value = args[++i];
                try {
                    createdSince = Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    err.printf("Invalid value for --created-since: %s%n", value);
                    return ParsedInput.invalid();
                }
            } else if ("--published-since".equals(arg)) {
                if (i + 1 >= args.length) {
                    err.println("Missing value for --published-since option.");
                    return ParsedInput.invalid();
                }
                String value = args[++i];
                try {
                    publishedSince = Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    err.printf("Invalid value for --published-since: %s%n", value);
                    return ParsedInput.invalid();
                }
            } else if ("--show-abstract".equals(arg) || "-a".equals(arg)) {
                showAbstract = true;
            } else if ("--search-mode".equals(arg) || "-s".equals(arg)) {
                if (i + 1 >= args.length) {
                    err.println("Missing value for --search-mode option.");
                    return ParsedInput.invalid();
                }
                String value = args[++i].trim().toLowerCase();
                searchMode = switch (value) {
                    case "title" -> OpenAlexClient.SearchMode.TITLE_ONLY;
                    case "abstract" -> OpenAlexClient.SearchMode.ABSTRACT_ONLY;
                    case "title-abstract", "title+abstract", "title_or_abstract" -> OpenAlexClient.SearchMode.TITLE_AND_ABSTRACT;
                    case "broad" -> OpenAlexClient.SearchMode.BROAD;
                    default -> {
                        err.println("Invalid search mode. Use broad, title, abstract, or title-abstract.");
                        yield null;
                    }
                };
                if (searchMode == null) {
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
        return ParsedInput.valid(query, perPage, languages, concepts, showAbstract, searchMode, createdSince, publishedSince);
    }

    private String formatWork(Work work, boolean showAbstract, int index) {
        String name = firstNonBlank(work.getDisplayName(), work.getTitle(), work.getId());
        String year = work.getPublicationYear() > 0 ? String.valueOf(work.getPublicationYear()) : "Unknown Year";
        String type = work.getType() != null ? work.getType() : "";
        String source = work.getPrimaryLocation() != null && work.getPrimaryLocation().getSource() != null
                ? work.getPrimaryLocation().getSource().getDisplayName()
                : "";
        String fulltextUrl = resolveFulltextUrl(work);
        String doi = work.getDoi() != null ? work.getDoi() : "";

        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%d. %s (%s)", index, name, year));
        if (!type.isBlank()) {
            builder.append(" [").append(type).append("]");
        }
        if (!source.isBlank()) {
            builder.append(" @ ").append(source);
        }
        builder.append(System.lineSeparator());

        appendDetail(builder, "DOI", doi);
        appendDetail(builder, "Full text", fulltextUrl);
        appendDetail(builder, "Publication date", formatDate(work.getPublicationDate()));
        appendDetail(builder, "Created", formatDate(work.getCreatedDate()));
        appendDetail(builder, "Updated", formatDate(work.getUpdatedDate()));
        if (work.getCitedByCount() > 0) {
            appendDetail(builder, "Cited by", String.valueOf(work.getCitedByCount()));
        }
        if (work.getOpenAccess() != null && work.getOpenAccess().isOa()) {
            appendDetail(builder, "OA", work.getOpenAccess().getOaStatus());
        }
        List<String> institutions = extractInstitutions(work);
        if (!institutions.isEmpty()) {
            appendDetail(builder, "Institutions", String.join("; ", institutions));
        }
        List<String> concepts = extractConcepts(work);
        if (!concepts.isEmpty()) {
            appendDetail(builder, "Concepts", String.join(", ", concepts));
        }
        if (showAbstract) {
            String abstractText = work.getAbstractText();
            if (abstractText != null && !abstractText.isBlank()) {
                appendDetail(builder, "Abstract", truncate(abstractText, 400));
            }
        }
        builder.append(System.lineSeparator());

        return builder.toString();
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

    private String resolveFulltextUrl(Work work) {
        if (work.getBestOALocation() != null) {
            String pdf = firstNonBlank(work.getBestOALocation().getPdfUrl());
            if (!pdf.isBlank()) {
                return pdf;
            }
            String landing = firstNonBlank(work.getBestOALocation().getLandingPageUrl());
            if (!landing.isBlank()) {
                return landing;
            }
        }
        if (work.getOpenAccess() != null) {
            String oaUrl = firstNonBlank(work.getOpenAccess().getOaUrl());
            if (!oaUrl.isBlank()) {
                return oaUrl;
            }
        }
        if (work.getPrimaryLocation() != null) {
            String pdf = firstNonBlank(work.getPrimaryLocation().getPdfUrl());
            if (!pdf.isBlank()) {
                return pdf;
            }
            String landing = firstNonBlank(work.getPrimaryLocation().getLandingPageUrl());
            if (!landing.isBlank()) {
                return landing;
            }
        }
        return "";
    }

    private void printUsage(PrintStream stream) {
        stream.println("Usage: openalex-cli [--per-page <1-200>] <search terms>");
        stream.println("Options:");
        stream.println("  --languages, -l <codes>      Comma-separated language codes to include (e.g. en,sv,de)");
        stream.println("  --concepts, -c <ids>         Comma-separated OpenAlex concept IDs (e.g. C555206,C17744445)");
        stream.println("  --created-since <days>       Filter by works created in the last N days");
        stream.println("  --published-since <days>     Filter by works published in the last N days");
        stream.println("  --per-page, -n <number>      Number of results per page (default 5, max 200)");
        stream.println("  --show-abstract, -a          Include abstract text (up to 400 characters)");
        stream.println("  --search-mode, -s <mode>     Search scope: broad (default), title, abstract, title-abstract");
        stream.println("Examples:");
        stream.println("  openalex-cli quantum computing");
        stream.println("  openalex-cli --per-page 3 \"graph neural networks\"");
        stream.println("  openalex-cli -l en,sv,da climate change");
        stream.println("  openalex-cli -s title --show-abstract theology");
    }

    private static class ParsedInput {
        private final boolean valid;
        private final String query;
        private final int perPage;
        private final List<String> languages;
        private final List<String> concepts;
        private final Integer createdSince;
        private final Integer publishedSince;
        private final boolean showAbstract;
        private final OpenAlexClient.SearchMode searchMode;

        private ParsedInput(boolean valid, String query, int perPage, List<String> languages, List<String> concepts,
                            boolean showAbstract, OpenAlexClient.SearchMode searchMode, Integer createdSince, Integer publishedSince) {
            this.valid = valid;
            this.query = query;
            this.perPage = perPage;
            this.languages = languages;
            this.concepts = concepts;
            this.showAbstract = showAbstract;
            this.searchMode = searchMode;
            this.createdSince = createdSince;
            this.publishedSince = publishedSince;
        }

        static ParsedInput invalid() {
            return new ParsedInput(false, null, DEFAULT_PAGE_SIZE, null, null, false,
                    OpenAlexClient.SearchMode.BROAD, null, null);
        }

        static ParsedInput valid(String query, int perPage, List<String> languages, List<String> concepts,
                                 boolean showAbstract, OpenAlexClient.SearchMode searchMode, Integer createdSince, Integer publishedSince) {
            return new ParsedInput(true, query, perPage, languages, concepts, showAbstract, searchMode, createdSince, publishedSince);
        }
    }

    private List<String> parseLanguages(String value) {
        String[] parts = value.split(",");
        List<String> languages = new ArrayList<>();
        for (String part : parts) {
            String code = part.trim().toLowerCase();
            if (!code.isEmpty()) {
                languages.add(code);
            }
        }
        return languages.stream().distinct().toList();
    }

    private List<String> parseConcepts(String value) {
        String[] parts = value.split(",");
        List<String> concepts = new ArrayList<>();
        for (String part : parts) {
            String concept = part.trim();
            if (!concept.isEmpty()) {
                concepts.add(concept);
            }
        }
        return concepts.stream().distinct().toList();
    }

    private List<String> extractInstitutions(Work work) {
        if (work.getAuthorships() == null) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (Authorship authorship : work.getAuthorships()) {
            if (authorship == null || authorship.getInstitutions() == null) {
                continue;
            }
            for (Authorship.Institution institution : authorship.getInstitutions()) {
                if (institution == null) {
                    continue;
                }
                String name = firstNonBlank(institution.getDisplayName());
                if (name.isBlank()) {
                    continue;
                }
                String country = firstNonBlank(institution.getCountryCode()).toUpperCase();
                String formatted = country.isBlank() ? name : name + " (" + country + ")";
                unique.add(formatted);
            }
        }
        return unique.stream().collect(Collectors.toList());
    }

    private List<String> extractConcepts(Work work) {
        if (work.getConcepts() == null) {
            return List.of();
        }
        return work.getConcepts().stream()
                .map(concept -> firstNonBlank(concept.getDisplayName(), concept.getId()))
                .filter(value -> !value.isBlank())
                .limit(10)
                .collect(Collectors.toList());
    }

    private void appendDetail(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append("   ").append(label).append(": ").append(value).append(System.lineSeparator());
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : "";
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }
}
