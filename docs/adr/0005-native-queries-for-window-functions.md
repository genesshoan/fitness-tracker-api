# ADR-0005: Use Native SQL Queries for Complex Statistical Queries

- **Status:** Accepted
- **Date:** 2026-09-10

## Context

The statistics module requires queries that operate on large amounts of aggregated workout data.

The application initially preferred JPQL because it provides a convenient abstraction over the database and allows most queries to be expressed in terms of entities and their relationships.

However, some of the queries required by the statistics module are not naturally expressible in JPQL.

In particular, the statistics calculations require PostgreSQL features such as window functions, including `ROW_NUMBER()` with `PARTITION BY`, as well as common table expressions (CTEs).

These features are useful for selecting and aggregating specific records within groups without requiring multiple queries or moving large amounts of data into the application.

For example, some statistics require selecting the best set for each exercise within each workout session before performing further calculations. Doing this efficiently requires operations that are much easier to express directly in SQL.

Attempting to express these queries through JPQL would either make the queries significantly more complicated or require additional queries and application-side processing.

The statistics module also contains several data-intensive queries where reducing unnecessary database round trips and unnecessary entity loading is important.

## Decision

Use **native SQL queries** for complex statistical queries that require database-specific functionality or cannot be expressed efficiently through JPQL.

JPQL remains the preferred approach for simpler queries where its abstraction provides clear benefits.

Native queries are therefore not intended to replace JPQL throughout the application. They are used selectively when the underlying SQL capabilities provide a meaningful advantage.

The statistics module uses native PostgreSQL queries when necessary, including features such as:

- Window functions such as `ROW_NUMBER()`.
- `PARTITION BY`.
- Common table expressions (CTEs).
- Database-side aggregation and calculations.
- Other PostgreSQL-specific functionality when it provides a significant benefit.

Query results are mapped to projections or dedicated DTO-oriented structures rather than loading full entity graphs when the operation only requires statistical data.

## Consequences

### Positive

- Complex statistical calculations can be performed directly in the database.
- PostgreSQL window functions can be used without fighting the limitations of JPQL.
- Large datasets can be processed closer to the data instead of being loaded into application memory.
- The number of database round trips can be reduced.
- Queries can take advantage of PostgreSQL-specific functionality.
- Projections can return only the data required by the statistics calculations.

### Negative

- Native SQL queries are coupled more closely to PostgreSQL.
- Queries are less portable to other database systems.
- Native SQL requires more knowledge of the underlying database.
- SQL queries are more verbose than equivalent simple JPQL queries.
- Database-specific behavior must be considered when testing and maintaining the queries.
- Developers need to maintain SQL separately from the Java entity model.

### Guidelines

Native queries should be used when they provide a clear technical benefit, rather than simply because SQL is more familiar or perceived to be faster.

Simple entity-oriented queries should continue to use JPQL when it provides a cleaner and sufficiently efficient solution.

The goal is to use the appropriate abstraction level for each query rather than enforcing a single query mechanism throughout the application.
