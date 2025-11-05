---
id: task-010
title: Add date filters to search
status: Done
assignee: []
created_date: '2025-10-18 10:00'
updated_date: '2025-11-05 21:49'
labels:
  - enhancement
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement the ability to filter OpenAlex works by creation date and publication date.

The user should be able to search for works:
- Created since a specific date or since a number of days.
- Published since a specific date or since a number of days.

This will involve:
- Adding `withFromCreatedDate(LocalDate date)` and `withFromPublicationDate(LocalDate date)` methods to `OpenAlexClient`.
- Adding convenience methods `withCreatedSince(int days)` and `withPublicationDateSince(int days)`.
- Updating the `buildFilterQuery` method in `OpenAlexClient` to include `from_created_date` and `from_publication_date` filters.
<!-- SECTION:DESCRIPTION:END -->
