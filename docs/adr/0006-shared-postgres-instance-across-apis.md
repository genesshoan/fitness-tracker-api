# ADR-0006: Use a Shared PostgreSQL Instance Across APIs

- **Status:** Accepted
- **Date:** 2026-09-03

## Context

The application is intended to be deployable on low-resource hardware, including a small home server.

The target hardware currently has limited resources, including approximately 4 GB of RAM. Because of these constraints, running a separate PostgreSQL instance for every API would introduce unnecessary resource overhead.

Each PostgreSQL instance requires its own database process, memory, connections, configuration, and other resources. Running multiple instances on the same small server would therefore be wasteful for the current deployment environment.

At the same time, the APIs should remain logically isolated from each other at the database level.

The deployment strategy therefore needs to balance resource usage with database isolation.

## Decision

Use a **single shared PostgreSQL instance** for multiple APIs.

Each API will have its own database and database user within the shared PostgreSQL instance.

The PostgreSQL instance itself will be managed independently from the individual API containers.

The API container images will therefore not include their own PostgreSQL instance. Instead, applications connect to the shared PostgreSQL service at runtime.

The intended production setup is:

- One PostgreSQL instance.
- One database per API.
- One database user per API.
- Separate credentials for each API.
- Each API only has access to its own database.

For example, an API should not connect using a shared database user with access to every application's data.

## Consequences

### Positive

- Significantly reduces resource usage on the target hardware.
- Avoids running multiple PostgreSQL processes on a resource-constrained server.
- Keeps each API logically isolated at the database level.
- Database credentials can be scoped to the database required by each API.
- PostgreSQL can be managed and backed up independently from the API containers.
- API containers remain smaller and focused only on running the application itself.
- Additional APIs can be deployed without requiring another PostgreSQL server process.

### Negative

- The PostgreSQL instance becomes a shared infrastructure dependency.
- A failure of the PostgreSQL instance can affect multiple APIs simultaneously.
- Resource contention between databases is possible.
- Database maintenance or upgrades can affect multiple APIs.
- The deployment architecture is less isolated than running a completely separate PostgreSQL instance for every API.

### Security Considerations

Although the PostgreSQL server is shared, database access should not be shared between applications.

Each API should use its own PostgreSQL user with only the permissions required by its own database.

Application credentials must be provided through deployment configuration or secrets rather than being embedded in container images or source code.

Sharing the PostgreSQL instance is therefore a resource optimization and does not imply sharing database credentials or application data between APIs.

### Future Considerations

If the deployment environment gains sufficient resources, individual PostgreSQL instances could be considered for applications that require stronger isolation or independent scaling.

For the current hardware constraints, the operational and resource cost of multiple PostgreSQL instances does not justify that additional isolation.
