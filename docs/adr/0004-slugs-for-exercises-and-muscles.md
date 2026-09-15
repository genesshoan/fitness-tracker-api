# ADR-0004: Use Slugs for Exercises and Muscles

- **Status:** Under review
- **Date:** 2026-06-19

## Context

The exercise and muscle resources can be identified using their database identifiers, but those identifiers are not particularly convenient for humans.

A slug provides a readable and URL-friendly representation of a resource. For example, an exercise could be identified by a value such as `bench-press` instead of its UUID.

This can make URLs and API requests easier to read and understand, especially for resources that are naturally identified by a meaningful name.

At the time, exercises and muscles were being developed as a separate module, making this a reasonable approach for those resources.

## Decision

Use slugs as an additional way to identify exercises and muscles.

Slugs should be human-readable, URL-friendly, and derived from the resource name.

UUIDs remain the primary identifiers for persistence and relationships between entities.

The slug is therefore treated as a domain-level identifier for external use rather than as a replacement for the entity's primary key.

## Consequences

### Positive

- URLs and API resources can be easier to read and understand.
- Exercises and muscles can be referenced using meaningful names instead of opaque UUIDs.
- Slugs provide a more user-friendly identifier for resources that have stable, human-readable names.

### Negative

- Slugs introduce additional uniqueness and validation requirements.
- Changes to the resource name may require updating the slug.
- The application has to maintain both the UUID and the slug.
- Using slugs consistently across the API adds additional complexity compared with simply using UUIDs.

### Current Status

This approach was only adopted for the exercise and muscle module and was not consistently used throughout the rest of the application.

As the project evolved, UUIDs became the primary way of identifying resources across the API. The long-term need for slugs is therefore under review.

If slugs are ultimately removed from the exercise and muscle domain, this ADR should remain as a record of the historical decision rather than being deleted. A subsequent ADR should document the removal and supersede this decision.
