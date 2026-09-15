# ADR-0001: Use UUID v7 as the Identifier Strategy

- **Status:** Accepted
- **Date:** 2026-06-05

## Context

The application needs a strategy for identifying persistent entities.

Using auto-incrementing integer IDs would be simple and efficient, but sequential IDs are easy to guess. If entity IDs are exposed through API endpoints, an attacker could potentially enumerate resources by incrementing an ID and attempting to access other records.

Since security is one of the main learning goals of this project, avoiding predictable identifiers is an important consideration.

UUIDs are a better fit for this requirement because they do not expose the sequential nature of the underlying records.

However, not all UUID versions are equally suitable for database indexes.

UUID v4 is completely random. While this makes it difficult to guess, its random distribution can result in poor index locality and increased index fragmentation as new records are inserted.

UUID v7 provides a time-ordered component, making newly generated UUIDs roughly sortable by creation time. This provides better locality for database indexes compared to fully random UUIDs while retaining the benefits of UUIDs as non-sequential identifiers.

Another consideration is where UUIDs should be generated.

PostgreSQL 18 supports UUID v7 and provides a `uuidv7()` function that can be used as a database-side default. However, relying exclusively on database-side generation creates a problem when entities are created and related within the application before they are persisted.

If the ID is only generated when the entity is inserted into the database, the application may not have access to that ID before the insert is flushed. This can force the service layer to explicitly flush the persistence context whenever the generated ID is needed immediately.

For example, when creating an entity and subsequently using its ID to establish another relationship, the application would have to rely on a database round trip just to obtain an identifier that could have been generated locally.

This also introduces a dependency on the developer remembering to flush the persistence context at the appropriate point. Forgetting to do so can result in references being created before the required identifier exists, potentially causing persistence or referential-integrity problems.

Therefore, the project needs an identifier strategy that provides the benefits of UUID v7 while allowing identifiers to exist in the application before the entity is persisted.

## Decision

Use UUID v7 as the identifier format for persistent entities.

UUID v7 was chosen instead of:

- Auto-incrementing integers, because sequential identifiers are predictable and can expose information about the underlying data.
- UUID v4, because its completely random distribution provides worse index locality and can increase index fragmentation.
- Database-specific sequential identifiers, because the application should be able to generate identifiers independently of the database.

UUIDs are generated on the application side when the entity is created, rather than relying exclusively on PostgreSQL to generate them during insertion.

The application uses the `uuid-creator` library to generate UUID v7 values:

`com.github.f4b6a3:uuid-creator:6.0.0`

This allows the entity ID to be available immediately when the entity is instantiated. The application can therefore use the identifier when creating related objects without first flushing the persistence context.

PostgreSQL is still configured with a UUID v7 default where appropriate. This provides a database-side fallback and keeps the database capable of generating valid identifiers independently of the application.

The specific decision to generate identifiers on the application side, including its interaction with JPA/Hibernate and database-generated identifiers, is documented in ADR-0003: Application-Side ID Generation.

## Consequences

### Positive

- API resources do not expose easily predictable sequential identifiers.
- UUID v7 provides better index locality than UUID v4.
- Entity IDs are available immediately when objects are created.
- Related entities can reference an ID without requiring a database flush first.
- The service layer does not need to explicitly flush the persistence context simply to obtain an identifier.
- Entity creation is less dependent on the developer remembering when a flush is required.
- UUIDs can be generated independently of the database.
- UUIDs remain globally unique without relying on a centralized database sequence.

### Negative

- UUIDs are larger than typical integer identifiers, increasing storage and index size.
- UUIDs are less human-friendly when debugging or manually interacting with API resources.
- UUID v7 generation requires an additional application dependency.
- The project is somewhat coupled to PostgreSQL because PostgreSQL's UUID v7 support influenced the database choice.
- The application and database both have the ability to generate IDs, so the two strategies must remain consistent.

### Security Considerations

UUIDs are intended to make resource identifiers difficult to guess, but they are not an authorization mechanism.

Every endpoint that exposes resources must still verify that the authenticated user is authorized to access the requested resource.

Using UUID v7 reduces the risk of trivial identifier enumeration compared with sequential IDs, but it does not prevent unauthorized access by itself.
