# Fitness Tracker API

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql&logoColor=white)
![CI](https://github.com/genesshoan/fitness-tracker-api/actions/workflows/ci.yml/badge.svg)

A production-minded REST API for tracking workouts, routines, body measurements, and training progress. This portfolio project focuses on clear domain boundaries, secure user-scoped data, realistic workout-session behavior, and a testable Spring Boot architecture that can support a future frontend.

## Highlights

- JWT authentication with short-lived access tokens and rotating, revocable refresh-token families
- Exercise and muscle catalog with cursor-based pagination and multi-field filtering
- User-owned routines with ordered exercises and category-specific metric validation
- Workout sessions that can be created from routines or built from scratch
- Implicit workout conveniences: position normalization, sequential set numbering, last-set reuse, and category defaults
- Progress records for weight and body-fat trends
- Statistics for volume, estimated 1RM, streaks, progression, and personal achievements
- OpenAPI/Swagger metadata and frontend-oriented module documentation
- Unit, repository, MVC, and PostgreSQL integration tests

## Domain Modules

| Module | Responsibility | Documentation |
| --- | --- | --- |
| `auth` / `security` | Registration, login, JWT validation, refresh rotation, logout, and revocation | [Auth docs](docs/auth/README.md) |
| `user` | User profile, credentials, and timezone-aware account data | [User docs](docs/user/README.md) |
| `exercise` | Read-only exercise and muscle catalog | [Exercise docs](docs/exercise/README.md) |
| `routine` | Reusable ordered workout plans | [Routine docs](docs/routine/README.md) |
| `workout` | Live and completed workout sessions, exercises, and sets | [Workout docs](docs/workout/README.md) |
| `progressrecord` | User body measurements and date-range queries | [Progress docs](docs/progress/README.md) |
| `stats` | Aggregated training metrics and achievements | [Stats docs](docs/stats/README.md) |

The complete endpoint inventory is available in [`docs/endpoints.md`](docs/endpoints.md). All API routes are versioned under `/api/v1`.

## Technical Decisions

### Modular Spring Boot design

The code is organized by business capability rather than by framework layer. Controllers expose HTTP contracts, services coordinate use cases, repositories isolate persistence, and DTOs keep API models separate from JPA entities. This keeps feature behavior discoverable and makes individual modules easier to extend.

### PostgreSQL with Flyway migrations

PostgreSQL is the primary database. Schema changes are versioned in `src/main/resources/db/migration`, while Hibernate is configured with `ddl-auto: none`. This makes database evolution explicit and keeps production schema management separate from application startup.

### JWT access tokens plus persistent refresh tokens

Access tokens remain stateless and are validated by the security filter. Refresh tokens are persisted so the API can rotate them, revoke a complete token family, track sessions, and detect reuse. This balances low-latency authenticated requests with server-side session control.

### Explicit domain invariants

The domain enforces ownership, soft deletion, ordered positions, unique routine names, category-specific exercise metrics, and immutable completed workout sessions. Workout operations normalize positions and set numbers on the server instead of trusting client-side ordering.

### Batch-oriented statistics and defaults

Statistics use JDBC projections and SQL window functions for aggregate work. Personal-record candidates for multiple exercises are loaded in one batch query. When a workout is created from scratch, prior completed-set defaults for all requested exercises are loaded in one query, then reused in memory.

### Testing against realistic infrastructure

The test suite includes unit tests for calculations and services, repository tests, web-layer tests, and PostgreSQL integration tests using Testcontainers. CI runs formatting checks and the full Gradle build on Java 21.

## Getting Started

### Prerequisites

- Java 21
- Docker and Docker Compose
- Git

### Configure the database

Copy the example environment file and adjust credentials if needed:

```bash
cp .env.example .env
docker compose up -d postgres
```

The default development profile expects PostgreSQL at `localhost:5432`. Flyway applies migrations automatically on startup.

### Run the application

```bash
./gradlew bootRun
```

The API starts with the `dev` profile when `SPRING_PROFILES_ACTIVE=dev` is loaded from `.env`. Do not use the example JWT secret in a real deployment.

### Run checks

```bash
./gradlew spotlessCheck test
```

The OpenAPI UI is available at `/swagger-ui/index.html` while the application is running, and the generated specification is available at `/v3/api-docs`.

## API Examples

Request examples for authentication, exercises, routines, and user operations are available in [`http/`](http/). The module documentation includes request/response shapes, business rules, frontend guidance, and diagrams.

## Repository Structure

```text
src/main/java/.../
├── auth/             Authentication and refresh-token lifecycle
├── common/           Shared errors, pagination, mappings, and utilities
├── exercise/         Exercise and muscle catalog
├── progressrecord/  Body-measurement tracking
├── routine/          Reusable workout plans
├── stats/            Aggregations and achievement calculations
├── user/             User profile and credentials
└── workout/          Sessions, session exercises, and sets
docs/                 Module references, diagrams, and frontend contracts
http/                 HTTP request examples
src/main/resources/   Profiles, seed data, and Flyway migrations
src/test/             Unit, repository, MVC, and integration tests
```

## License

This project is licensed under the [MIT License](LICENSE).
