# OpenAlex4J

OpenAlex4J is a lightweight Java 21 client for the [OpenAlex](https://openalex.org) scholarly metadata API.  
It offers:

- A fluent `OpenAlexClient` for programmatic access.
- Built-in helpers for language and concept filtering.
- Tunable search scopes (title, abstract, broad).
- A shaded CLI (`openalex4j-…-cli.jar`) for quick queries from the terminal.

The project uses SLF4J for logging, Jackson for JSON mapping, and Java’s built-in `HttpClient`.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [OpenAlexClient API](#openalexclient-api)
   - [Creating a client](#creating-a-client)
   - [Search modes](#search-modes)
   - [Language & concept filters](#language--concept-filters)
   - [Handling responses](#handling-responses)
3. [CLI](#cli)
   - [Building](#building-the-cli-jar)
   - [Flags](#flags)
   - [Examples](#examples)
4. [Logging](#logging)
5. [Testing](#testing)

---

## Quick Start

```bash
# build the project
./mvnw clean package

# run CLI (default broad search)
java -jar target/openalex4j-1.0-SNAPSHOT-cli.jar "quantum computing"

# run tests
./mvnw test
```

The default build produces both `openalex4j-1.0-SNAPSHOT.jar` (library) and a shaded CLI jar under `target/`.

---

## OpenAlexClient API

### Creating a client

```java
import org.openalex4j.OpenAlexClient;
import org.openalex4j.Work;

OpenAlexClient client = OpenAlexClient.create();

// simple search (default mode, default language filter)
List<Work> works = client.searchWorks("machine learning", 20);
```

`OpenAlexClient.create()` configures:

- Java 21 `HttpClient` with a 10s connect timeout.
- Jackson `ObjectMapper` with `JavaTimeModule` and tolerant deserialization.
- Default language filter: `sv`, `da`, `no`, `de`, `fr`, `en`.

### Search modes

Specify which fields OpenAlex should match against:

```java
client = client.withSearchMode(OpenAlexClient.SearchMode.TITLE_ONLY);
List<Work> titleMatches = client.searchWorks("quantum computing", 10);
```

Supported modes:

| Mode                     | Behaviour                                                     |
|--------------------------|---------------------------------------------------------------|
| `BROAD` (default)        | Regular `search=…`, OpenAlex matches across multiple fields. |
| `TITLE_ONLY`             | `search=title:"term"` + filter `title.search:term`.           |
| `ABSTRACT_ONLY`          | `search=abstract:"term"` + filter `abstract.search:term`.     |
| `TITLE_AND_ABSTRACT`     | `search=title:"term" OR abstract:"term"` (OR logic).          |

Passing an empty/blank query throws `IllegalArgumentException`.

### Language & concept filters

```java
client = client
        .withAllowedLanguages(List.of("en", "sv"))
        .withConcepts(List.of("C555206", "C17744445")); // theology + religion concepts

List<Work> filtered = client
        .withSearchMode(OpenAlexClient.SearchMode.TITLE_AND_ABSTRACT)
        .searchWorks("religious studies", 25);
```

- Languages are normalized to lowercase and deduplicated.
- Concepts accept raw OpenAlex concept IDs.
- Filters are combined using OpenAlex’s `filter=` syntax (`language:` and `concept.id:`).

### Handling responses

Each `Work` contains the primary OpenAlex metadata:

- `displayName`, `title`, `publicationYear`, `publicationDate`, `type`.
- `ids` map (OpenAlex, DOI, MAG, etc.).
- `primaryLocation`, `bestOALocation`, `openAccess`.
- `citedByCount`, `relevanceScore`.
- `concepts` (list with id/display name level).
- `authorships` (authors, institutions, countries).
- `abstractText()` helper reconstructs the abstract from the inverted index.

Example:

```java
Work w = filtered.getFirst();
System.out.println(w.getDisplayName());
System.out.println("Institutions: " + w.getAuthorships().stream()
    .flatMap(a -> Optional.ofNullable(a.getInstitutions()).stream().flatMap(List::stream))
    .map(Authorship.Institution::getDisplayName)
    .distinct()
    .toList());
System.out.println("Abstract: " + w.getAbstractText());
```

If OpenAlex returns an error or parsing fails, an `OpenAlexException` (unchecked) is thrown.

---

## CLI

### Building the CLI jar

```bash
./mvnw package
ls target/openalex4j-1.0-SNAPSHOT-cli.jar
```

The shaded jar includes dependencies and uses the same defaults as the Java API (languages `sv|da|no|de|fr|en`, broad search).

### Flags

Run `java -jar openalex4j-…-cli.jar --help` for usage. Key options:

| Flag                     | Description                                                                          |
|--------------------------|--------------------------------------------------------------------------------------|
| `--languages`, `-l`      | Comma-separated language codes (`en,sv,de`). Overrides default language whitelist.   |
| `--concepts`, `-c`       | Comma-separated OpenAlex concept IDs (`C555206`).                                    |
| `--per-page`, `-n`       | Page size (1–200). Default 5.                                                        |
| `--search-mode`, `-s`    | `broad` (default), `title`, `abstract`, `title-abstract`.                            |
| `--show-abstract`, `-a`  | Print up to 400 characters of the reconstructed abstract.                            |

The CLI output displays:

- Title, year, type and primary venue.
- DOI/full text URL.
- Citation count, OA status.
- Institutions (with country code if available).
- Concepts (up to 10).
- Optional abstract snippet.

### Examples

```bash
# Broad search with defaults (languages sv/da/no/de/fr/en)
java -jar target/openalex4j-1.0-SNAPSHOT-cli.jar "quantum computing"

# Title-only search limited to English & Swedish and theology concepts
java -jar target/openalex4j-1.0-SNAPSHOT-cli.jar \
  --languages en,sv \
  --concepts C555206,C17744445 \
  --search-mode title \
  --show-abstract \
  "church"

# Abstract-only search in English, Spanish, Portuguese (no concept filter)
java -jar target/openalex4j-1.0-SNAPSHOT-cli.jar \
  --languages en,es,pt \
  --search-mode abstract \
  "ritual violence"
```

CLI exit codes:

| Code | Meaning                          |
|------|----------------------------------|
| 0    | OK (results printed or “no results”). |
| 1    | Invalid arguments (usage error). |
| 2    | OpenAlex request failed or other runtime error. |

---

## Logging

- The library uses SLF4J (`slf4j-api` + `slf4j-simple` by default).
- Client logs HTTP requests at debug level, warnings on non‑2xx responses, and errors for exceptions.
- In production, swap `slf4j-simple` for your preferred backend (Logback, Log4J2, etc.).

Example (Logback):

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.6</version>
</dependency>
<!-- exclude slf4j-simple in pom -->
```

---

## Testing

```bash
# run unit tests (mocks http client)
./mvnw test

# run live integration tests (requires OPENALEX_LIVE_TEST=true)
OPENALEX_LIVE_TEST=true ./mvnw test -Dtest=OpenAlexClientIntegrationTest
```

Integration tests are skipped unless the environment variable is set (they hit the real OpenAlex API).

For local CLI smoke tests:

```bash
./mvnw package
java -jar target/openalex4j-1.0-SNAPSHOT-cli.jar --search-mode title "theology"
```

---

## Additional Notes

- Default filters are conservative (Nordic + major EU languages). Use `--languages` or `withAllowedLanguages` to widen.
- Concepts (`withConcepts` or `--concepts`) accept multiple IDs and are AND-ed with the query.
- `work.getAbstractText()` reconstructs the text; if you need raw inverted index use `getAbstractInvertedIndex()`.
- For heavy usage, consider adding caching or respect OpenAlex rate limits (`mailto` query param in manual API calls).

Contributions and bug reports are welcome. Use tasks in `backlog/` directory to stay aligned with the MCP workflow.
