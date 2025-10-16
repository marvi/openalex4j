# OpenAlex API Findings

This document summarizes the findings from the OpenAlex API documentation, focusing on searching and retrieving the latest documents based on a search pattern.

## API Overview

*   **Authentication:** No authentication is required, but providing an email address in the `mailto` parameter is recommended for better performance and to be added to the "polite pool" of users.
*   **Rate Limits:** There is a rate limit of 100,000 requests per day per user.
*   **API Endpoints:** The main endpoint for searching works is `https://api.openalex.org/works`.

## Searching and Filtering

The API provides powerful filtering and searching capabilities.

*   **`search` parameter:** This parameter allows for full-text search on the title, abstract, and full text of the works. It supports boolean operators like `AND`, `OR`, and `NOT`.
*   **`filter` parameter:** This parameter allows for more specific filtering based on various attributes of the works, such as `publication_year`, `authorships.institutions.lineage`, and more.

## Retrieving the Latest Documents

To retrieve the latest documents, you can use a combination of filtering and sorting.

*   **`sort` parameter:** You can sort the results by `publication_date` in descending order to get the latest documents first.
    *   Example: `sort=publication_date:desc`
*   **`filter` by `publication_year`:** You can filter the results to only include documents from a specific year or a range of years.
    *   Example: `filter=publication_year:2023` or `filter=publication_year:>2022`
*   **`from_created_date` and `from_updated_date`:** The premium API offers these filters to retrieve hourly updates, which is ideal for finding the absolute latest documents.

## Paging

The API uses cursor paging to handle large result sets.

*   **`per-page` parameter:** This parameter specifies the number of results per page (e.g., 200).
*   **`cursor` parameter:** To retrieve all documents matching a search, you use the `cursor` parameter. The first request uses `*`, and subsequent requests use the `next_cursor` value from the previous response's `meta` object.

## Example Query

Here is an example of a query to retrieve the latest works from a specific institution:

```
https://api.openalex.org/works?filter=authorships.institutions.lineage:i129801699,publication_year:>2022&sort=publication_date:desc
```

This query retrieves works from the institution with ID `i129801699` published after 2022, sorted by publication date in descending order.

## Specification and Plan

This section outlines the specification and plan for building the OpenAlex4J library.

### Core Classes

*   **`OpenAlexClient`:** The main entry point for interacting with the OpenAlex API. It will handle the HTTP requests and deserialization of the responses.
    *   `create()`: A static factory method to create an instance of the client.
    *   `searchWorks(String query)`: A method to search for works based on a query string. This will be the primary method for the initial implementation.
*   **`Work`:** A class representing a work in OpenAlex. It will contain fields for the work's ID, title, publication date, authors, etc.
*   **`Author`:** A class representing an author of a work.
*   **`OpenAlexResponse<T>`:** A generic class to represent the response from the OpenAlex API. It will contain the metadata and a list of results.

### Implementation Plan

1.  **Create the core classes:** Create the `OpenAlexClient`, `Work`, `Author`, and `OpenAlexResponse` classes with the fields and methods defined above.
2.  **Implement the `searchWorks` method:**
    *   This method will take a query string as a parameter.
    *   It will construct the URL for the API request, including the `search` and `sort` parameters.
    *   It will use the `jakarta.ws.rs.client.Client` to make the HTTP GET request.
    *   It will use the `com.fasterxml.jackson.databind.ObjectMapper` to deserialize the JSON response into an `OpenAlexResponse<Work>` object.
    *   It will return a `List<Work>` to the caller.
3.  **Add unit tests:**
    *   Create a test class for the `OpenAlexClient`.
    *   Write a test case for the `searchWorks` method to verify that it correctly parses the response and returns the expected results.
    *   Mock the HTTP client to avoid making actual API calls in the tests.
4.  **Add error handling:**
    *   Implement error handling for HTTP errors and JSON parsing errors.
5.  **Implement paging:**
    *   Add support for cursor paging to allow users to retrieve all the results for a query.
    *   This will involve adding a method to the `OpenAlexClient` that takes a cursor as a parameter.

### Example Usage

```java
OpenAlexClient client = OpenAlexClient.create();
List<Work> works = client.searchWorks("machine learning");
for (Work work : works) {
    System.out.println(work.getTitle());
}
```