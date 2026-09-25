# AGENTS.md

## Project Overview

**Stack**: Java 21, Spring Boot 3.5.14, Spring Data JPA, Spring Security, JWT, MapStruct, Lombok, H2/PostgreSQL, Flyway, TestContainers, JUnit 5.

## Main Stack & Versions

- **Language**: Java 21 (Spring Boot 3.5.14)
- **Framework**: Spring Boot 3.5.14
- **Database**: H2 (dev), PostgreSQL (prod)
- **Security**: Spring Security + JWT (jjwt 0.12.6)
- **Data Access**: Spring Data JPA
- **Mapping**: MapStruct 1.6.3
- **Lombok**: 4.x (annotation processors for getters/setters)
- **Testing**: JUnit 5, TestContainers (JUnit Jupiter, org.testcontainers:postgresql)
- **Formatting**: Spotless (palantirJavaFormat 2.97.0, importOrder, removeUnusedImports, trimTrailingWhitespace, endWithNewline)

## Naming Conventions

### Packages
- Root: `dev/genesshoan/fitnesstrackerapi/`
- Modules: `auth`, `exercise`, `common`
- Sub-packages: `domain`, `dto`, `service`, `error`

### Classes
- **Package-level**: CamelCase (e.g., `TokenRepository`, `AuthService`, `ExerciseFinder`)
- **DTOs**: `*RequestDTO`, `*ResponseDTO` (e.g., `LoginRequestDTO`, `TokenResponseDTO`)
- **Services**: `*Service` (e.g., `AuthService`, `JwtService`)
- **Repositories**: `*Repository` (e.g., `TokenRepository`)
- **Entities**: `*Entity` (e.g., `BaseEntity`, `ExerciseMetrics`)

### Exceptions
- Suffix with `Exception` (e.g., `ResourceNotFoundException`, `InvalidCredentialsException`, `ResourceAlreadyExistsException`)
- Custom exception classes in `common/error/exception/`

### Controllers
- REST controllers in `*Controller` (e.g., `AuthController`)

### Methods
- Verb-noun pattern (e.g., `create`, `getById`, `update`, `delete`)
- Consistent naming: `getAll`, `getById`, `save`, `delete`

## Folder Structure

```
src/main/java/
├── dev/genesshoan/fitnesstrackerapi/
│   ├── auth/
│   │   ├── AuthController.java          # REST endpoints
│   │   ├── AuthService.java             # Business logic
│   │   └── TokenRepository.java         # JPA repository
│   ├── exercise/
│   │   ├── ExerciseFinder.java          # Exercise-related queries
│   │   ├── Category.java                # Exercise categories
│   │   ├── Difficulty.java              # Exercise difficulty levels
│   │   └── ExerciseMuscle.java          # Muscle mapping
│   ├── common/
│   │   ├── error/
│   │   │   ├── BadRequestException.java
│   │   │   ├── InvalidCredentialsException.java
│   │   │   ├── InvalidJwtException.java
│   │   │   ├── ResourceAlreadyExistsException.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   ├── UnauthorizedException.java
│   │   │   └── ValidationException.java
│   │   └── handler/
│   │       ├── GlobalExceptionHandler.java
│   │       ├── HttpExceptionHandler.java
│   │       ├── ValidationExceptionHandler.java
│   │       └── ProblemDetailUtils.java
│   └── domain/
│       ├── BaseEntity.java               # Common base class
│       ├── ExerciseMetrics.java          # Metrics for exercises
│       └── ExerciseMuscle.java           # Muscle associations
```

## CRUD Endpoint Pattern

### Authentication Flow
- **POST `/auth/login`** – Login request → authenticate → return JWT token
- **POST `/auth/register`** – Register user → create account → return token
- **GET `/auth/me`** – Retrieve current user info
- **POST `/auth/refresh`** – Refresh JWT token

### Exercise Management Flow
- **GET `/exercise/find`** – Search exercises by category/difficulty
- **GET `/exercise/{id}`** – Fetch exercise details
- **POST `/exercise`** – Create new exercise
- **PUT `/exercise/{id}`** – Update exercise
- **DELETE `/exercise/{id}`** – Delete exercise

### Shared Patterns
- **DTOs** for request/response (e.g., `ExerciseFinderRequestDTO`, `ExerciseResponseDTO`)
- **Custom exceptions** for validation/not-found scenarios (`ResourceNotFoundException`)
- **Global exception handler** centralizes error responses
- **MapStruct** for converting DTO ↔ Entity mappings
- **Spotless** for consistent code formatting

## Build, Test, and Lint Commands

### Build
```bash
# Clean and build the project
./gradlew clean build

# Run tests
./gradlew test

# Generate seeds (DB initialization)
./gradlew seed
```

### Lint & Format
```bash
# Format with Spotless (Java)
./gradlew spotlessCheck
./gradlew spotlessFormat

# Check formatting
./gradlew spotlessCheck --quiet
```

### Test
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "dev.genesshoan.fitnesstrackerapi.*"
```

## Style Rules

### Error Handling
- All domain errors extend `Exception` (e.g., `ResourceNotFoundException`)
- Custom exceptions defined in `common/error/exception/`
- Global exception handler (`GlobalExceptionHandler`) maps exceptions to HTTP status codes
- Proper HTTP status codes: `200 OK`, `201 Created`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `500 Internal Server Error`

### Validation
- Input validation via DTOs and `@Valid` annotations
- Custom exception classes for validation failures (`InvalidCredentialsException`, `InvalidJwtException`)
- Centralized exception handling prevents unhandled exceptions

### Code Formatting
- **Palantir Java Format** (version 2.97.0) enforces:
  - Import order (standard library → third-party → project)
  - Remove unused imports
  - Trim trailing whitespace
  - End files with newline
- All Java files formatted by `spotlessFormat`

### Naming
- **Classes**: PascalCase (`TokenRepository`)
- **Methods**: camelCase (`getAll`, `create`, `update`)
- **Variables**: camelCase with clear purpose (`userService`, `exerciseList`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_EXERCISES`, `JWT_SECRET`)

### Testing
- Unit tests for services and repositories
- Integration tests with TestContainers for DB interactions
- Mock dependencies for service-layer tests
- Test data seeding via `seed` task

## Key Files to Reference

- **Auth Controller**: `AuthController.java` – REST endpoints for auth
- **Token Repository**: `TokenRepository.java` – JPA interface for token operations
- **JWT Service**: `JwtService.java` – JWT generation/validation
- **Base Entity**: `BaseEntity.java` – Common fields (id, createdAt, updatedAt)
- **Exercise Finder**: `ExerciseFinder.java` – Exercise querying logic
- **Exception Hierarchy**: `common/error/exception/*.java`
- **Global Handler**: `GlobalExceptionHandler.java` – Centralized error mapping
